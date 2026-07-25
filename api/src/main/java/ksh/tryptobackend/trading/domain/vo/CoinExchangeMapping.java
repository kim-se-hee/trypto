package ksh.tryptobackend.trading.domain.vo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CoinExchangeMapping {

    private final Map<Long, Long> exchangeCoinIdByCoinId;
    private final Set<Long> suspendedCoinIds;

    public CoinExchangeMapping(Map<Long, Long> exchangeCoinIdByCoinId, Set<Long> suspendedCoinIds) {
        this.exchangeCoinIdByCoinId = Map.copyOf(exchangeCoinIdByCoinId);
        this.suspendedCoinIds = Set.copyOf(suspendedCoinIds);
    }

    public Long getExchangeCoinId(Long coinId) {
        return exchangeCoinIdByCoinId.get(coinId);
    }

    public boolean isTradable(Long coinId) {
        return exchangeCoinIdByCoinId.containsKey(coinId) && !suspendedCoinIds.contains(coinId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoinExchangeMapping that = (CoinExchangeMapping) o;
        return Objects.equals(exchangeCoinIdByCoinId, that.exchangeCoinIdByCoinId)
                && Objects.equals(suspendedCoinIds, that.suspendedCoinIds);
    }

    @Override
    public int hashCode() {
        return Objects.hash(exchangeCoinIdByCoinId, suspendedCoinIds);
    }
}
