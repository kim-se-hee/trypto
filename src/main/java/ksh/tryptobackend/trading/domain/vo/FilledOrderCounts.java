package ksh.tryptobackend.trading.domain.vo;

import java.util.Map;
import java.util.Objects;

public class FilledOrderCounts {

    private final Map<Long, Integer> values;

    public FilledOrderCounts(Map<Long, Integer> values) {
        this.values = Map.copyOf(values);
    }

    public Map<Long, Integer> toMap() {
        return values;
    }

    public int getCount(Long walletId) {
        return values.getOrDefault(walletId, 0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilledOrderCounts that = (FilledOrderCounts) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
