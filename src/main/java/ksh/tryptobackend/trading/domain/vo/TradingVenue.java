package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.util.Objects;

public class TradingVenue {

    private final BigDecimal feeRate;
    private final Long baseCurrencyCoinId;
    private final OrderAmountPolicy orderAmountPolicy;

    public TradingVenue(BigDecimal feeRate, Long baseCurrencyCoinId, OrderAmountPolicy orderAmountPolicy) {
        this.feeRate = feeRate;
        this.baseCurrencyCoinId = baseCurrencyCoinId;
        this.orderAmountPolicy = orderAmountPolicy;
    }

    public static TradingVenue of(BigDecimal feeRate, Long baseCurrencyCoinId, boolean domestic) {
        OrderAmountPolicy policy = domestic ? OrderAmountPolicy.DOMESTIC : OrderAmountPolicy.OVERSEAS;
        return new TradingVenue(feeRate, baseCurrencyCoinId, policy);
    }

    public Fee calculateFee(BigDecimal filledAmount) {
        return Fee.calculate(filledAmount, feeRate);
    }

    public void validateOrderAmount(BigDecimal amount) {
        orderAmountPolicy.validate(amount);
    }

    public Long baseCurrencyCoinId() {
        return baseCurrencyCoinId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TradingVenue that)) return false;
        return feeRate.compareTo(that.feeRate) == 0
            && Objects.equals(baseCurrencyCoinId, that.baseCurrencyCoinId)
            && orderAmountPolicy == that.orderAmountPolicy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(feeRate.stripTrailingZeros(), baseCurrencyCoinId, orderAmountPolicy);
    }
}
