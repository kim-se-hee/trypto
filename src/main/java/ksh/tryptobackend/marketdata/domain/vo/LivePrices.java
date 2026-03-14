package ksh.tryptobackend.marketdata.domain.vo;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class LivePrices {

    private final Map<Long, BigDecimal> values;

    public LivePrices(Map<Long, BigDecimal> values) {
        this.values = Map.copyOf(values);
    }

    public BigDecimal getPrice(Long exchangeCoinId) {
        return values.get(exchangeCoinId);
    }

    public Map<Long, BigDecimal> toMap() {
        return values;
    }

    public Set<Long> exchangeCoinIds() {
        return values.keySet();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LivePrices that = (LivePrices) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
