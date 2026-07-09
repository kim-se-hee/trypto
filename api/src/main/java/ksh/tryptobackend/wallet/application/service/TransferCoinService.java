package ksh.tryptobackend.wallet.application.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.wallet.application.port.in.TransferCoinUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.command.TransferCoinCommand;
import ksh.tryptobackend.wallet.application.port.out.TransferCommandPort;
import ksh.tryptobackend.wallet.application.port.out.WalletCommandPort;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import ksh.tryptobackend.wallet.domain.model.Transfer;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TransferCoinService implements TransferCoinUseCase {

    private final TransferCommandPort transferCommandPort;
    private final WalletQueryPort walletQueryPort;
    private final WalletCommandPort walletCommandPort;

    private final Clock clock;

    @Override
    @Transactional
    public Transfer transferCoin(TransferCoinCommand command) {
        Optional<Transfer> completed =
                transferCommandPort.findByIdempotencyKey(command.idempotencyKey());
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

        BigDecimal sourceAvailable =
                walletQueryPort.getAvailableBalance(command.fromWalletId(), command.coinId());
        Transfer transfer = Transfer.create(command, sourceAvailable, LocalDateTime.now(clock));

        walletCommandPort.deductBalance(
                transfer.getFromWalletId(), transfer.getCoinId(), transfer.getAmount());
        walletCommandPort.addBalance(
                transfer.getToWalletId(), transfer.getCoinId(), transfer.getAmount());
        return transferCommandPort.save(transfer);
    }
}
