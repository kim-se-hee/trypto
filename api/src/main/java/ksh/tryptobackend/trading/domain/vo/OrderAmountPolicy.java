package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderAmountPolicy {
    DOMESTIC(new BigDecimal("5000"), new BigDecimal("1000000000"), 0),
    OVERSEAS(new BigDecimal("5"), null, 8);

    private final BigDecimal minAmount;
    private final BigDecimal maxAmount;
    // 기축통화 소수 자릿수. 수수료를 이 자릿수로 내림 절삭한다 (KRW=정수 원, USDT=8자리)
    private final int quoteScale;

    public static OrderAmountPolicy of(String baseCurrencySymbol) {
        return switch (baseCurrencySymbol) {
            case "KRW" -> DOMESTIC;
            case "USDT" -> OVERSEAS;
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
