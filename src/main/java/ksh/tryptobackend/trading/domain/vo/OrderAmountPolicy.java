package ksh.tryptobackend.trading.domain.vo;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

import java.math.BigDecimal;

public record OrderAmountPolicy(BigDecimal minAmount, BigDecimal maxAmount) {

    private static final OrderAmountPolicy KRW = new OrderAmountPolicy(
        new BigDecimal("5000"), new BigDecimal("1000000000"));
    private static final OrderAmountPolicy USDT = new OrderAmountPolicy(
        new BigDecimal("5"), null);

    public static OrderAmountPolicy of(String baseCurrencySymbol) {
        return switch (baseCurrencySymbol) {
            case "KRW" -> KRW;
            case "USDT" -> USDT;
            default -> throw new CustomException(ErrorCode.UNSUPPORTED_BASE_CURRENCY);
        };
    }

    public void validate(BigDecimal amount) {
        if (amount.compareTo(minAmount) < 0) {
            throw new CustomException(ErrorCode.BELOW_MIN_ORDER_AMOUNT);
        }
        if (maxAmount != null && amount.compareTo(maxAmount) > 0) {
            throw new CustomException(ErrorCode.ABOVE_MAX_ORDER_AMOUNT);
        }
    }
}
