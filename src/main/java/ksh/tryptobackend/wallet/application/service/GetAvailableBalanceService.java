package ksh.tryptobackend.wallet.application.service;

import ksh.tryptobackend.wallet.application.port.in.GetAvailableBalanceUseCase;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceOperationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class GetAvailableBalanceService implements GetAvailableBalanceUseCase {

    private final WalletBalanceOperationPort walletBalanceOperationPort;

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return walletBalanceOperationPort.getAvailableBalance(walletId, coinId);
    }
}
