package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.util.Objects;

public record Price(BigDecimal value) {

    public static Price of(BigDecimal value) {
        return new Price(value);
    }

    public static Price zero() {
        return new Price(BigDecimal.ZERO);
    }

    public Money times(Quantity quantity) {
        return Money.of(value.multiply(quantity.value()));
    }

    public boolean isHigherThan(Price other) {
        return value.compareTo(other.value) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Price price)) return false;
        return value.compareTo(price.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }
}
