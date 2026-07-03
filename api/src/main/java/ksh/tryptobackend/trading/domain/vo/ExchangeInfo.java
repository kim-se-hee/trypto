package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.util.Objects;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public class ExchangeInfo {

    private final BigDecimal feeRate;
    private final BigDecimal minOrderAmount;
    private final BigDecimal maxOrderAmount;

    public ExchangeInfo(BigDecimal feeRate, BigDecimal minOrderAmount, BigDecimal maxOrderAmount) {
        this.feeRate = feeRate;
        this.minOrderAmount = minOrderAmount;
        this.maxOrderAmount = maxOrderAmount;
    }

    public Fee calculateFee(Money filledAmount) {
        return Fee.calculate(filledAmount, feeRate);
    }

    public void validateOrderAmount(BigDecimal amount) {
        if (amount.compareTo(minOrderAmount) < 0) {
            throw new CustomException(ErrorCode.BELOW_MIN_ORDER_AMOUNT);
        }
        if (maxOrderAmount != null && amount.compareTo(maxOrderAmount) > 0) {
            throw new CustomException(ErrorCode.ABOVE_MAX_ORDER_AMOUNT);
        }
    }

    public BigDecimal feeRate() {
        return feeRate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ExchangeInfo that)) return false;
        return feeRate.compareTo(that.feeRate) == 0
                && minOrderAmount.compareTo(that.minOrderAmount) == 0
                && (maxOrderAmount == null
                        ? that.maxOrderAmount == null
                        : that.maxOrderAmount != null
                                && maxOrderAmount.compareTo(that.maxOrderAmount) == 0);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                feeRate.stripTrailingZeros(),
                minOrderAmount.stripTrailingZeros(),
                maxOrderAmount == null ? null : maxOrderAmount.stripTrailingZeros());
    }
}
