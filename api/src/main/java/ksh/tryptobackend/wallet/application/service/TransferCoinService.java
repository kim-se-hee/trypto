package ksh.tryptobackend.wallet.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.application.port.in.TransferCoinUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.command.TransferCoinCommand;
import ksh.tryptobackend.wallet.application.port.out.TransferCommandPort;
import ksh.tryptobackend.wallet.application.port.out.TransferQueryPort;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceCommandPort;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceQueryPort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.domain.model.Transfer;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;
import ksh.tryptobackend.wallet.domain.model.WalletBalances;
import ksh.tryptobackend.wallet.domain.service.CoinTransferrer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferCoinService implements TransferCoinUseCase {

    private final TransferQueryPort transferQueryPort;
    private final TransferCommandPort transferCommandPort;
    private final WalletQueryPort walletQueryPort;
    private final WalletBalanceQueryPort walletBalanceQueryPort;
    private final WalletBalanceCommandPort walletBalanceCommandPort;

    private final CoinTransferrer coinTransferrer;

    private final Clock clock;

    @Override
    @Transactional
    public Transfer transferCoin(TransferCoinCommand command) {
        Optional<Transfer> completed =
                transferQueryPort.findByIdempotencyKey(command.idempotencyKey());
        if (completed.isPresent()) {
            return completed.orElseThrow();
        }

        Wallet source =
                walletQueryPort
                        .findById(command.fromWalletId())
                        .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
        Wallet destination =
                walletQueryPort
                        .findById(command.toWalletId())
                        .orElseThrow(() -> new CustomException(ErrorCode.WALLET_NOT_FOUND));
        source.verifySameRoundAs(destination);

        Transfer transfer = Transfer.create(command, LocalDateTime.now(clock));

        List<WalletBalance> lockedBalances =
                walletBalanceQueryPort.getAllByCoinIdAndWalletIdsWithLock(
                        command.coinId(), List.of(command.fromWalletId(), command.toWalletId()));
        WalletBalances balances = new WalletBalances(lockedBalances);
        coinTransferrer.transfer(
                balances.getByWalletId(command.fromWalletId()),
                balances.getByWalletId(command.toWalletId()),
                command.amount());

        walletBalanceCommandPort.saveAll(lockedBalances);
        return transferCommandPort.save(transfer);
    }
}
