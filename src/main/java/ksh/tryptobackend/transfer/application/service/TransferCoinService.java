package ksh.tryptobackend.transfer.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.transfer.application.port.in.TransferCoinUseCase;
import ksh.tryptobackend.transfer.application.port.in.dto.command.TransferCoinCommand;
import ksh.tryptobackend.transfer.application.port.out.TransferDepositPort;
import ksh.tryptobackend.transfer.application.port.out.TransferExchangeCoinChainPort;
import ksh.tryptobackend.transfer.application.port.out.TransferExchangePort;
import ksh.tryptobackend.transfer.application.port.out.TransferPersistencePort;
import ksh.tryptobackend.transfer.application.port.out.TransferWalletPort;
import ksh.tryptobackend.transfer.application.port.out.TransferWithdrawalFeePort;
import ksh.tryptobackend.transfer.application.port.out.dto.TransferDepositAddressInfo;
import ksh.tryptobackend.transfer.application.port.out.dto.TransferExchangeCoinChainInfo;
import ksh.tryptobackend.transfer.application.port.out.dto.TransferExchangeInfo;
import ksh.tryptobackend.transfer.application.port.out.dto.TransferWalletInfo;
import ksh.tryptobackend.transfer.application.port.out.dto.TransferWithdrawalFeeInfo;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferFailureReason;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferCoinService implements TransferCoinUseCase {

    private final TransferPersistencePort transferPersistencePort;
    private final TransferWalletPort walletPort;
    private final TransferDepositPort depositPort;
    private final TransferWithdrawalFeePort withdrawalFeePort;
    private final TransferExchangeCoinChainPort chainPort;
    private final TransferExchangePort exchangePort;
    private final Clock clock;

    @Override
    @Transactional
    public Transfer transferCoin(TransferCoinCommand command) {
        return transferPersistencePort.findByIdempotencyKey(command.clientTransferId())
            .orElseGet(() -> executeTransfer(command));
    }

    private Transfer executeTransfer(TransferCoinCommand command) {
        TransferWalletInfo wallet = walletPort.getWallet(command.fromWalletId());
        validateNotBaseCurrency(wallet.exchangeId(), command.coinId());
        validateSourceChainSupport(wallet.exchangeId(), command.coinId(), command.chain());
        TransferWithdrawalFeeInfo feeInfo = withdrawalFeePort.getWithdrawalFee(
            wallet.exchangeId(), command.coinId(), command.chain());
        validateMinWithdrawal(command.amount(), feeInfo.minWithdrawal());
        validateBalance(command.fromWalletId(), command.coinId(), command.amount(), feeInfo.fee());

        Transfer transfer = determineOutcome(command, wallet, feeInfo.fee());
        applyBalanceChanges(transfer);
        return transferPersistencePort.save(transfer);
    }

    private void validateNotBaseCurrency(Long exchangeId, Long coinId) {
        TransferExchangeInfo exchangeInfo = exchangePort.getExchangeDetail(exchangeId);
        if (exchangeInfo.baseCurrencyCoinId().equals(coinId)) {
            throw new CustomException(ErrorCode.BASE_CURRENCY_NOT_TRANSFERABLE);
        }
    }

    private void validateSourceChainSupport(Long exchangeId, Long coinId, String chain) {
        chainPort.findByExchangeIdAndCoinIdAndChain(exchangeId, coinId, chain)
            .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_CHAIN));
    }

    private void validateMinWithdrawal(BigDecimal amount, BigDecimal minWithdrawal) {
        if (amount.compareTo(minWithdrawal) < 0) {
            throw new CustomException(ErrorCode.BELOW_MIN_WITHDRAWAL);
        }
    }

    private void validateBalance(Long walletId, Long coinId, BigDecimal amount, BigDecimal fee) {
        BigDecimal required = amount.add(fee);
        BigDecimal available = walletPort.getAvailableBalance(walletId, coinId);
        if (available.compareTo(required) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    private Transfer determineOutcome(TransferCoinCommand command, TransferWalletInfo wallet,
                                       BigDecimal fee) {
        LocalDateTime now = LocalDateTime.now(clock);

        var depositAddress = depositPort.findByRoundIdAndChainAndAddress(
            wallet.roundId(), command.chain(), command.toAddress());
        if (depositAddress.isEmpty()) {
            return Transfer.frozen(command.clientTransferId(), command.fromWalletId(),
                command.coinId(), command.chain(), command.toAddress(), command.toTag(),
                command.amount(), fee, TransferFailureReason.WRONG_ADDRESS, now);
        }

        TransferDepositAddressInfo destAddress = depositAddress.get();
        TransferWalletInfo destWallet = walletPort.getWallet(destAddress.walletId());

        var destChainInfo = chainPort.findByExchangeIdAndCoinIdAndChain(
            destWallet.exchangeId(), command.coinId(), command.chain());
        if (destChainInfo.isEmpty()) {
            return Transfer.frozen(command.clientTransferId(), command.fromWalletId(),
                command.coinId(), command.chain(), command.toAddress(), command.toTag(),
                command.amount(), fee, TransferFailureReason.WRONG_CHAIN, now);
        }

        TransferExchangeCoinChainInfo destChain = destChainInfo.get();
        if (destChain.tagRequired() && (command.toTag() == null || command.toTag().isBlank())) {
            return Transfer.frozen(command.clientTransferId(), command.fromWalletId(),
                command.coinId(), command.chain(), command.toAddress(), command.toTag(),
                command.amount(), fee, TransferFailureReason.MISSING_TAG, now);
        }

        return Transfer.success(command.clientTransferId(), command.fromWalletId(),
            destAddress.walletId(), command.coinId(), command.chain(),
            command.toAddress(), command.toTag(), command.amount(), fee, now);
    }

    private void applyBalanceChanges(Transfer transfer) {
        BigDecimal totalDeduction = transfer.getAmount().add(transfer.getFee());

        switch (transfer.getStatus()) {
            case SUCCESS -> {
                walletPort.deductBalance(transfer.getFromWalletId(), transfer.getCoinId(), totalDeduction);
                walletPort.addBalance(transfer.getToWalletId(), transfer.getCoinId(), transfer.getAmount());
            }
            case FROZEN -> {
                walletPort.lockBalance(transfer.getFromWalletId(), transfer.getCoinId(), totalDeduction);
            }
            default -> throw new IllegalStateException("Unexpected transfer status: " + transfer.getStatus());
        }
    }
}
