package ksh.tryptobackend.wallet.application.service;

import java.time.Clock;
import java.time.LocalDateTime;
import ksh.tryptobackend.common.idempotency.IdempotencyKeyCommandPort;
import ksh.tryptobackend.common.idempotency.IdempotencyResourceType;
import ksh.tryptobackend.wallet.application.port.in.TransferCoinUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.command.TransferCoinCommand;
import ksh.tryptobackend.wallet.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.wallet.application.port.out.TransferCommandPort;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceCommandPort;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceQueryPort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.domain.model.Transfer;
import ksh.tryptobackend.wallet.domain.model.TransferBalances;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import ksh.tryptobackend.wallet.domain.service.CoinTransferrer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferCoinService implements TransferCoinUseCase {

    private final IdempotencyKeyCommandPort idempotencyKeyCommandPort;
    private final TransferCommandPort transferCommandPort;
    private final WalletQueryPort walletQueryPort;
    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final WalletBalanceQueryPort walletBalanceQueryPort;
    private final WalletBalanceCommandPort walletBalanceCommandPort;

    private final CoinTransferrer coinTransferrer;

    private final Clock clock;

    @Override
    @Transactional
    public Transfer transferCoin(TransferCoinCommand command) {
        LocalDateTime now = LocalDateTime.now(clock);
        String idempotencyKey = command.idempotencyKey();
        idempotencyKeyCommandPort.preempt(idempotencyKey, IdempotencyResourceType.TRANSFER, now);

        Wallet source = walletQueryPort.getById(command.fromWalletId());
        Wallet destination = walletQueryPort.getById(command.toWalletId());
        source.verifySameRoundAs(destination);
        source.verifyOwnedBy(command.requesterId(), investmentRoundQueryPort.getOwnerId(source.getRoundId()));

        Transfer transfer = Transfer.create(command, now);

        TransferBalances balances = walletBalanceQueryPort.getTransferBalancesWithLock(
                command.coinId(), command.fromWalletId(), command.toWalletId());
        coinTransferrer.transfer(balances, command.amount());

        walletBalanceCommandPort.saveAll(balances.toList());
        Transfer saved = transferCommandPort.save(transfer);
        idempotencyKeyCommandPort.linkResource(idempotencyKey, saved.getTransferId());
        return saved;
    }
}
