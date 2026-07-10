package ksh.tryptobackend.wallet.application.service;

import java.math.BigDecimal;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceQueryPort;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAvailableBalanceService implements GetAvailableBalanceUseCase {

    private final WalletBalanceQueryPort walletBalanceQueryPort;

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return walletBalanceQueryPort
                .findByWalletIdAndCoinId(walletId, coinId)
                .map(WalletBalance::getAvailable)
                .orElse(BigDecimal.ZERO);
    }

    @Override
    public BigDecimal getTotalBalance(Long walletId, Long coinId) {
        return walletBalanceQueryPort
                .findByWalletIdAndCoinId(walletId, coinId)
                .map(WalletBalance::totalBalance)
                .orElse(BigDecimal.ZERO);
    }
}
