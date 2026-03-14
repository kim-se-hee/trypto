package ksh.tryptobackend.marketdata.domain.vo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class CoinSymbols {

    private final Map<Long, String> values;

    public CoinSymbols(Map<Long, String> values) {
        this.values = Map.copyOf(values);
    }

    public String getSymbol(Long coinId) {
        return values.get(coinId);
    }

    public Map<Long, String> toMap() {
        return values;
    }

    public Set<Long> coinIds() {
        return values.keySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoinSymbols that = (CoinSymbols) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
