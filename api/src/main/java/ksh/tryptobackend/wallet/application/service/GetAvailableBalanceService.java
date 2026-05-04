package ksh.tryptobackend.wallet.application.service;

import java.math.BigDecimal;
import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.out.WalletQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetAvailableBalanceService implements GetAvailableBalanceUseCase {

    private final WalletQueryPort walletQueryPort;

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return walletQueryPort.getAvailableBalance(walletId, coinId);
    }
}
