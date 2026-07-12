package ksh.tryptobackend.investmentround.adapter.out.service;

import java.time.LocalDateTime;
import ksh.tryptobackend.investmentround.domain.service.SeedWalletProvisioner;
import ksh.tryptobackend.investmentround.domain.vo.SeedAllocation;
import ksh.tryptobackend.wallet.application.port.in.CreateWalletWithBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.command.CreateWalletWithBalanceCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeedWalletProvisionerImpl implements SeedWalletProvisioner {

    private final CreateWalletWithBalanceUseCase createWalletWithBalanceUseCase;

    @Override
    public Long provision(Long roundId, SeedAllocation allocation, LocalDateTime createdAt) {
        return createWalletWithBalanceUseCase.createWalletWithBalance(new CreateWalletWithBalanceCommand(
                roundId, allocation.exchangeId(), allocation.baseCurrencyCoinId(), allocation.amount(), createdAt));
    }
}
