package ksh.tryptobackend.investmentround.adapter.out.service;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.investmentround.domain.service.FundsDepositor;
import ksh.tryptobackend.wallet.application.port.in.ApplyBalanceChangesUseCase;
import ksh.tryptobackend.wallet.application.port.in.dto.BalanceChangeType;
import ksh.tryptobackend.wallet.application.port.in.dto.command.BalanceChangeCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FundsDepositorImpl implements FundsDepositor {

    private final ApplyBalanceChangesUseCase applyBalanceChangesUseCase;

    @Override
    public void deposit(Long walletId, Long coinId, BigDecimal amount) {
        applyBalanceChangesUseCase.applyBalanceChanges(
                walletId,
                List.of(new BalanceChangeCommand(BalanceChangeType.ADD_AVAILABLE, coinId, amount)));
    }
}
