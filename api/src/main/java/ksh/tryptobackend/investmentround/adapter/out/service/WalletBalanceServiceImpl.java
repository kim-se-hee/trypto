package ksh.tryptobackend.investmentround.adapter.out.service;

import java.math.BigDecimal;
import ksh.tryptobackend.investmentround.domain.service.WalletBalanceService;
import ksh.tryptobackend.wallet.application.port.in.ManageWalletBalanceUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("investmentroundWalletBalanceServiceImpl")
@RequiredArgsConstructor
public class WalletBalanceServiceImpl implements WalletBalanceService {

    private final ManageWalletBalanceUseCase manageWalletBalanceUseCase;

    @Override
    public void addAvailable(Long walletId, Long coinId, BigDecimal amount) {
        manageWalletBalanceUseCase.addBalance(walletId, coinId, amount);
    }
}
