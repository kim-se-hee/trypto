package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.ranking.application.port.out.BalanceQueryPort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockBalanceQueryAdapter implements BalanceQueryPort {

    private final Map<String, BigDecimal> balances = new ConcurrentHashMap<>();

    @Override
    public BigDecimal getAvailableBalance(Long walletId, Long coinId) {
        return balances.getOrDefault(key(walletId, coinId), BigDecimal.ZERO);
    }

    public void setBalance(Long walletId, Long coinId, BigDecimal balance) {
        balances.put(key(walletId, coinId), balance);
    }

    public void clear() {
        balances.clear();
    }

    private String key(Long walletId, Long coinId) {
        return walletId + ":" + coinId;
    }
}
