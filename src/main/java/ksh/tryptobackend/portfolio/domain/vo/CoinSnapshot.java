package ksh.tryptobackend.portfolio.domain.vo;

import java.math.BigDecimal;
import java.util.Objects;

public record CoinSnapshot(
        String symbol,
        String name,
        BigDecimal currentPrice
) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CoinSnapshot that = (CoinSnapshot) o;
        return Objects.equals(symbol, that.symbol)
                && Objects.equals(name, that.name)
                && currentPrice.compareTo(that.currentPrice) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, name, currentPrice);
    }
}
