package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.util.Objects;

public record Fee(Money amount, BigDecimal rate) {

    public static Fee calculate(Money filledAmount, BigDecimal feeRate) {
        Money feeAmount = Money.of(filledAmount.value().multiply(feeRate));
        return new Fee(feeAmount, feeRate);
    }

    public static Fee of(Money amount, BigDecimal rate) {
        return new Fee(amount, rate);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fee fee)) return false;
        return amount.equals(fee.amount) && rate.compareTo(fee.rate) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, rate.stripTrailingZeros());
    }
}
