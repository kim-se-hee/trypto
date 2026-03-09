package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.wallet.application.port.in.CreateWalletWithBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.command.CreateWalletWithBalanceCommand;
import ksh.tryptobackend.wallet.application.port.out.WalletCommandPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateWalletWithBalanceService implements CreateWalletWithBalanceUseCase {

    private final WalletCommandPort walletCommandPort;

    @Override
    @Transactional
    public Long createWalletWithBalance(CreateWalletWithBalanceCommand command) {
        return walletCommandPort.createWalletWithBalance(
            command.roundId(), command.exchangeId(), command.baseCurrencyCoinId(),
            command.initialAmount(), command.createdAt());
    }
}
