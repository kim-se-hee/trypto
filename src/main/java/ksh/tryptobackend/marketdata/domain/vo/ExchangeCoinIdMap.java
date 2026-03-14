package ksh.tryptobackend.marketdata.domain.vo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ExchangeCoinIdMap {

    private final Map<Long, Long> coinIdToExchangeCoinId;

    public ExchangeCoinIdMap(Map<Long, Long> coinIdToExchangeCoinId) {
        this.coinIdToExchangeCoinId = Map.copyOf(coinIdToExchangeCoinId);
    }

    public Long getExchangeCoinId(Long coinId) {
        return coinIdToExchangeCoinId.get(coinId);
    }

    public Set<Long> exchangeCoinIds() {
        return Set.copyOf(coinIdToExchangeCoinId.values());
    }

    public Map<Long, Long> toMap() {
        return coinIdToExchangeCoinId;
    }

    public Set<Long> coinIds() {
        return coinIdToExchangeCoinId.keySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeCoinIdMap that = (ExchangeCoinIdMap) o;
        return Objects.equals(coinIdToExchangeCoinId, that.coinIdToExchangeCoinId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(coinIdToExchangeCoinId);
    }
}
