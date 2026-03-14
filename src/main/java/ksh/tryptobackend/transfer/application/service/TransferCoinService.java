package ksh.tryptobackend.transfer.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinChainUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindWithdrawalFeeUseCase;
import ksh.tryptobackend.transfer.application.port.in.TransferCoinUseCase;
import ksh.tryptobackend.transfer.application.port.in.dto.command.TransferCoinCommand;
import ksh.tryptobackend.transfer.application.port.out.TransferCommandPort;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferBalanceChange;
import ksh.tryptobackend.transfer.domain.vo.TransferDestination;
import ksh.tryptobackend.transfer.domain.vo.TransferFailureReason;
import ksh.tryptobackend.transfer.domain.vo.TransferSourceExchange;
import ksh.tryptobackend.transfer.domain.vo.TransferWallet;
import ksh.tryptobackend.transfer.domain.vo.WithdrawalCondition;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.ResolveTransferDestinationUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.result.TransferDestinationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransferCoinService implements TransferCoinUseCase {

    private final TransferCommandPort transferCommandPort;
    private final FindWalletUseCase findWalletUseCase;
    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;
    private final ManageWalletBalanceUseCase manageWalletBalanceUseCase;
    private final ResolveTransferDestinationUseCase resolveTransferDestinationUseCase;
    private final FindWithdrawalFeeUseCase findWithdrawalFeeUseCase;
    private final FindExchangeCoinChainUseCase findExchangeCoinChainUseCase;
    private final FindExchangeDetailUseCase findExchangeDetailUseCase;
    private final Clock clock;

    @Override
    @Transactional
    public Transfer transferCoin(TransferCoinCommand command) {
        return transferCommandPort.findByIdempotencyKey(command.idempotencyKey())
            .orElseGet(() -> executeTransfer(command));
    }

    private Transfer executeTransfer(TransferCoinCommand command) {
        Long coinId = command.coinId();
        String chain = command.chain();

        TransferWallet wallet = getWallet(command.fromWalletId());
        Long sourceExchangeId = wallet.exchangeId();

        TransferSourceExchange sourceExchange = getExchangeDetail(sourceExchangeId);
        sourceExchange.validateTransferable(coinId);
        validateSourceChainSupport(sourceExchangeId, coinId, chain);

        WithdrawalCondition condition = getWithdrawalCondition(sourceExchangeId, coinId, chain);
        condition.validateMinWithdrawal(command.amount());
        condition.validateSufficientBalance(
            getAvailableBalanceUseCase.getAvailableBalance(command.fromWalletId(), coinId),
            command.amount());

        TransferDestination destination = resolveDestination(wallet.roundId(), coinId, chain,
            command.toAddress(), command.toTag());
        Transfer transfer = Transfer.create(command.idempotencyKey(), command.fromWalletId(),
            coinId, chain, command.toAddress(), command.toTag(),
            command.amount(), condition.fee(), destination, LocalDateTime.now(clock));
        applyBalanceChanges(transfer);
        return transferCommandPort.save(transfer);
    }

    private TransferWallet getWallet(Long walletId) {
        return findWalletUseCase.findById(walletId)
            .map(r -> new TransferWallet(r.walletId(), r.roundId(), r.exchangeId()))
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    private TransferSourceExchange getExchangeDetail(Long exchangeId) {
        return findExchangeDetailUseCase.findExchangeDetail(exchangeId)
            .map(d -> new TransferSourceExchange(d.baseCurrencyCoinId(), d.domestic()))
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
    }

    private WithdrawalCondition getWithdrawalCondition(Long exchangeId, Long coinId, String chain) {
        return findWithdrawalFeeUseCase.findByExchangeIdAndCoinIdAndChain(exchangeId, coinId, chain)
            .map(r -> new WithdrawalCondition(r.fee(), r.minWithdrawal()))
            .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_CHAIN));
    }

    private void validateSourceChainSupport(Long exchangeId, Long coinId, String chain) {
        findExchangeCoinChainUseCase.findByExchangeIdAndCoinIdAndChain(exchangeId, coinId, chain)
            .orElseThrow(() -> new CustomException(ErrorCode.UNSUPPORTED_CHAIN));
    }

    private TransferDestination resolveDestination(Long roundId, Long coinId, String chain,
                                                   String toAddress, String toTag) {
        TransferDestinationResult result = resolveTransferDestinationUseCase.resolveDestination(
            roundId, coinId, chain, toAddress, toTag);
        if (result.isResolved()) {
            return new TransferDestination.Resolved(result.walletId());
        }
        return new TransferDestination.Failed(TransferFailureReason.valueOf(result.failureReason()));
    }

    private void applyBalanceChanges(Transfer transfer) {
        for (TransferBalanceChange change : transfer.planBalanceChanges()) {
            applyBalanceChange(change);
        }
    }

    private void applyBalanceChange(TransferBalanceChange change) {
        switch (change) {
            case TransferBalanceChange.Deduct d ->
                manageWalletBalanceUseCase.deductBalance(d.walletId(), d.coinId(), d.amount());
            case TransferBalanceChange.Add a ->
                manageWalletBalanceUseCase.addBalance(a.walletId(), a.coinId(), a.amount());
            case TransferBalanceChange.Lock l ->
                manageWalletBalanceUseCase.lockBalance(l.walletId(), l.coinId(), l.amount());
        }
    }
}
