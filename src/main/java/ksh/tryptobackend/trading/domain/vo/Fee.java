package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class Fee {

    private final BigDecimal amount;
    private final BigDecimal rate;

    private Fee(BigDecimal amount, BigDecimal rate) {
        this.amount = amount;
        this.rate = rate;
    }

    public static Fee calculate(BigDecimal filledAmount, BigDecimal feeRate) {
        BigDecimal feeAmount = filledAmount.multiply(feeRate)
            .setScale(8, RoundingMode.FLOOR);
        return new Fee(feeAmount, feeRate);
    }

    public static Fee of(BigDecimal amount, BigDecimal rate) {
        return new Fee(amount, rate);
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getRate() {
        return rate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fee fee)) return false;
        return amount.compareTo(fee.amount) == 0 && rate.compareTo(fee.rate) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), rate.stripTrailingZeros());
    }
}
