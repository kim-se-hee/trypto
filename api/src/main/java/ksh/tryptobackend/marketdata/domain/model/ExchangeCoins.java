package ksh.tryptobackend.marketdata.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ExchangeCoins {

    private final List<ExchangeCoin> values;

    public ExchangeCoins(List<ExchangeCoin> values) {
        this.values = List.copyOf(values);
    }

    public Set<Long> coinIds() {
        return values.stream().map(ExchangeCoin::coinId).collect(Collectors.toSet());
    }

    public Set<Long> exchangeCoinIds() {
        return values.stream().map(ExchangeCoin::exchangeCoinId).collect(Collectors.toSet());
    }

    public List<ExchangeCoin> values() {
        return values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeCoins that = (ExchangeCoins) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
