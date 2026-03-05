package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.ranking.application.port.out.BalanceQueryPort;
import ksh.tryptobackend.wallet.application.port.out.WalletBalanceOperationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component("rankingBalanceQueryAdapter")
@RequiredArgsConstructor
public class BalanceQueryAdapter implements BalanceQueryPort {

    private final WalletBalanceOperationPort walletBalanceOperationPort;

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return walletBalanceOperationPort.getAvailableBalance(walletId, coinId);
    }
}
