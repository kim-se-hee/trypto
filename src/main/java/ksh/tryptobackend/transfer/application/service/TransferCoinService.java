package ksh.tryptobackend.transfer.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.transfer.application.port.in.TransferCoinUseCase;
import ksh.tryptobackend.transfer.application.port.in.dto.command.TransferCoinCommand;
import ksh.tryptobackend.transfer.application.port.out.TransferCommandPort;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferBalanceChange;
import ksh.tryptobackend.transfer.domain.vo.TransferWallet;
import ksh.tryptobackend.wallet.application.port.in.FindWalletUseCase;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
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

    private final Clock clock;

    @Override
    @Transactional
    public Transfer transferCoin(TransferCoinCommand command) {
        return transferCommandPort.findByIdempotencyKey(command.idempotencyKey())
            .orElseGet(() -> executeTransfer(command));
    }

    private Transfer executeTransfer(TransferCoinCommand command) {
        TransferWallet fromWallet = getWallet(command.fromWalletId());
        TransferWallet toWallet = getWallet(command.toWalletId());
        validateSameRound(fromWallet, toWallet);
        validateSufficientBalance(command);

        Transfer transfer = Transfer.create(command.idempotencyKey(), command.fromWalletId(),
            command.toWalletId(), command.coinId(), command.amount(), LocalDateTime.now(clock));
        applyBalanceChanges(transfer);
        return transferCommandPort.save(transfer);
    }

    private TransferWallet getWallet(Long walletId) {
        return findWalletUseCase.findById(walletId)
            .map(r -> new TransferWallet(r.walletId(), r.roundId(), r.exchangeId()))
            .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
    }

    private void validateSameRound(TransferWallet fromWallet, TransferWallet toWallet) {
        if (!fromWallet.roundId().equals(toWallet.roundId())) {
            throw new CustomException(ErrorCode.DIFFERENT_ROUND_TRANSFER);
        }
    }

    private void validateSufficientBalance(TransferCoinCommand command) {
        var available = getAvailableBalanceUseCase.getAvailableBalance(
            command.fromWalletId(), command.coinId());
        if (available.compareTo(command.amount()) < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
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
        }
    }
}
