package ksh.tryptobackend.investmentround.adapter.out.service;

import java.math.BigDecimal;
import ksh.tryptobackend.investmentround.domain.service.FundsDepositor;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FundsDepositorImpl implements FundsDepositor {

    private final ManageWalletBalanceUseCase manageWalletBalanceUseCase;

    @Override
    public void deposit(Long walletId, Long coinId, BigDecimal amount) {
        manageWalletBalanceUseCase.addBalance(walletId, coinId, amount);
    }
}
