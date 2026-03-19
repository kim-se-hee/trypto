package ksh.tryptobackend.marketdata.domain.vo;

import java.util.Map;
import java.util.Objects;

public class TickerSnapshots {

    private final Map<Long, TickerSnapshot> values;

    public TickerSnapshots(Map<Long, TickerSnapshot> values) {
        this.values = Map.copyOf(values);
    }

    public TickerSnapshot getSnapshot(Long exchangeCoinId) {
        return values.getOrDefault(exchangeCoinId, TickerSnapshot.EMPTY);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TickerSnapshots that = (TickerSnapshots) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
