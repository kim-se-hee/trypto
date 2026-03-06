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
import ksh.tryptobackend.transfer.domain.vo.TransferDepositAddress;
import ksh.tryptobackend.transfer.domain.vo.TransferWallet;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferBalanceChange;
import ksh.tryptobackend.transfer.domain.vo.TransferDestination;
import ksh.tryptobackend.transfer.domain.vo.TransferDestinationChain;
import ksh.tryptobackend.transfer.domain.vo.TransferFailureReason;
import ksh.tryptobackend.transfer.domain.vo.TransferSourceExchange;
import ksh.tryptobackend.transfer.domain.vo.WithdrawalCondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

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
        return transferPersistencePort.findByIdempotencyKey(command.idempotencyKey())
            .orElseGet(() -> executeTransfer(command));
    }

    private Transfer executeTransfer(TransferCoinCommand command) {
        Long coinId = command.coinId();
        String chain = command.chain();

        TransferWallet wallet = walletPort.getWallet(command.fromWalletId());
        Long sourceExchangeId = wallet.exchangeId();

        TransferSourceExchange sourceExchange = exchangePort.getExchangeDetail(sourceExchangeId);
        sourceExchange.validateTransferable(coinId);
        validateSourceChainSupport(sourceExchangeId, coinId, chain);

        WithdrawalCondition condition = withdrawalFeePort.getWithdrawalFee(
            sourceExchangeId, coinId, chain);
        condition.validateMinWithdrawal(command.amount());
        condition.validateSufficientBalance(
            walletPort.getAvailableBalance(command.fromWalletId(), coinId),
            command.amount());

        TransferDestination destination = resolveDestination(command, wallet, coinId, chain);
        Transfer transfer = Transfer.create(command.idempotencyKey(), command.fromWalletId(),
            coinId, chain, command.toAddress(), command.toTag(),
            command.amount(), condition.fee(), destination, LocalDateTime.now(clock));
        applyBalanceChanges(transfer);
        return transferPersistencePort.save(transfer);
    }

    private void validateSourceChainSupport(Long exchangeId, Long coinId, String chain) {
        chainPort.findByExchangeIdAndCoinIdAndChain(exchangeId, coinId, chain)
            .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_CHAIN));
    }

    private TransferDestination resolveDestination(TransferCoinCommand command,
                                                   TransferWallet wallet,
                                                   Long coinId, String chain) {
        Optional<TransferDepositAddress> depositAddress = depositPort.findByRoundIdAndChainAndAddress(
            wallet.roundId(), chain, command.toAddress());
        if (depositAddress.isEmpty()) {
            return new TransferDestination.Failed(TransferFailureReason.WRONG_ADDRESS);
        }

        TransferDepositAddress destAddress = depositAddress.get();
        TransferWallet destWallet = walletPort.getWallet(destAddress.walletId());

        Optional<TransferDestinationChain> destChainInfo = chainPort.findByExchangeIdAndCoinIdAndChain(
            destWallet.exchangeId(), coinId, chain);
        if (destChainInfo.isEmpty()) {
            return new TransferDestination.Failed(TransferFailureReason.WRONG_CHAIN);
        }

        TransferDestinationChain destChain = destChainInfo.get();
        if (destChain.isMissingRequiredTag(command.toTag())) {
            return new TransferDestination.Failed(TransferFailureReason.MISSING_TAG);
        }

        return new TransferDestination.Resolved(destAddress.walletId());
    }

    private void applyBalanceChanges(Transfer transfer) {
        for (TransferBalanceChange change : transfer.planBalanceChanges()) {
            applyBalanceChange(change);
        }
    }

    private void applyBalanceChange(TransferBalanceChange change) {
        switch (change) {
            case TransferBalanceChange.Deduct d ->
                walletPort.deductBalance(d.walletId(), d.coinId(), d.amount());
            case TransferBalanceChange.Add a ->
                walletPort.addBalance(a.walletId(), a.coinId(), a.amount());
            case TransferBalanceChange.Lock l ->
                walletPort.lockBalance(l.walletId(), l.coinId(), l.amount());
        }
    }
}
