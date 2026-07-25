package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public record Quantity(BigDecimal value) {

    private static final int SCALE = 8;

    public Quantity {
        value = value.setScale(SCALE, RoundingMode.FLOOR);
    }

    public static Quantity of(BigDecimal value) {
        return new Quantity(value);
    }

    public static Quantity zero() {
        return new Quantity(BigDecimal.ZERO);
    }

    public static Quantity from(BigDecimal amount, Price price) {
        BigDecimal result = amount.divide(price.value(), SCALE, RoundingMode.FLOOR);
        return new Quantity(result);
    }

    public Quantity plus(Quantity other) {
        return new Quantity(value.add(other.value));
    }

    public Quantity minus(Quantity other) {
        return new Quantity(value.subtract(other.value));
    }

    public boolean isPositive() {
        return value.compareTo(BigDecimal.ZERO) > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Quantity quantity)) return false;
        return value.compareTo(quantity.value) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value.stripTrailingZeros());
    }
}
