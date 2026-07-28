package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

/**
 * 기축통화 금액을 원화로 환산하는 시세. 원화 거래소는 항등 환산(1)이다.
 * 시세가 없으면 0 을 담아 두고, 환산이 실제로 필요한 시점(금액 > 0)에만 오류를 낸다.
 */
public record KrwConversionRate(BigDecimal value) {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    public static KrwConversionRate identity() {
        return new KrwConversionRate(BigDecimal.ONE);
    }

    public BigDecimal convert(BigDecimal amount) {
        if (amount.compareTo(ZERO) == 0) {
            return ZERO;
        }
        if (value == null || value.compareTo(ZERO) <= 0) {
            throw new CustomException(ErrorCode.PRICE_NOT_AVAILABLE);
        }
        return amount.multiply(value);
    }
}
