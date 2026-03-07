package ksh.tryptobackend.portfolio.adapter.out;

import ksh.tryptobackend.portfolio.application.port.out.BalanceQueryPort;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class BalanceQueryAdapter implements BalanceQueryPort {

    private final GetAvailableBalanceUseCase getAvailableBalanceUseCase;

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return getAvailableBalanceUseCase.getAvailableBalance(walletId, coinId);
    }
}
