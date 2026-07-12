package ksh.tryptobackend.wallet.application.service;

import java.util.List;
import ksh.tryptobackend.wallet.application.port.in.CreateWalletWithBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.command.CreateWalletWithBalanceCommand;
import ksh.tryptobackend.wallet.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceCommandPort;
import ksh.tryptobackend.wallet.application.port.out.WalletCommandPort;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CreateWalletWithBalanceService implements CreateWalletWithBalanceUseCase {

    private final WalletCommandPort walletCommandPort;
    private final WalletBalanceCommandPort walletBalanceCommandPort;
    private final MarketDataQueryPort marketDataQueryPort;

    @Override
    @Transactional
    public Long createWalletWithBalance(CreateWalletWithBalanceCommand command) {
        Wallet wallet = walletCommandPort.save(
                Wallet.create(command.roundId(), command.exchangeId(), command.initialAmount(), command.createdAt()));

        List<Long> tradableCoinIds = marketDataQueryPort.findCoinIdsByExchange(command.exchangeId());
        walletBalanceCommandPort.saveAll(wallet.openBalances(tradableCoinIds, command.baseCurrencyCoinId()));
        return wallet.getWalletId();
    }
}
