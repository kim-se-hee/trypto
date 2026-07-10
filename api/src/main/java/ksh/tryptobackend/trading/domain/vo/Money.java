package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Money(BigDecimal value) {

    private static final int SCALE = 8;

    public Money {
        value = value.setScale(SCALE, RoundingMode.FLOOR);
    }

    public static Money of(BigDecimal value) {
        return new Money(value);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO);
    }

    public Money plus(Money other) {
        return new Money(value.add(other.value));
    }

    public Money minus(Money other) {
        return new Money(value.subtract(other.value));
    }

    public Money times(BigDecimal ratio) {
        return new Money(value.multiply(ratio));
    }

    public Price dividedBy(Quantity quantity) {
        return Price.of(value.divide(quantity.value(), SCALE, RoundingMode.FLOOR));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Money money)) return false;
        return value.compareTo(money.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }
}
