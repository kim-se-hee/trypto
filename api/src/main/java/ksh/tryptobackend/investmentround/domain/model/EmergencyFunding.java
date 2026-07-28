package ksh.tryptobackend.investmentround.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * amount 는 투입 대상 거래소의 기축통화 단위, krwConvertedAmount 는 투입 시점 시세로 환산한 원화다.
 * 한도 검증과 총 투입금 집계는 환산액을 쓴다. 이후 시세가 변해도 환산액은 변하지 않는다.
 */
public record EmergencyFunding(
        Long id, Long exchangeId, BigDecimal amount, BigDecimal krwConvertedAmount, LocalDateTime createdAt) {

    public static EmergencyFunding create(
            Long exchangeId, BigDecimal amount, BigDecimal krwConvertedAmount, LocalDateTime createdAt) {
        return new EmergencyFunding(null, exchangeId, amount, krwConvertedAmount, createdAt);
    }

    public static EmergencyFunding reconstitute(
            Long id, Long exchangeId, BigDecimal amount, BigDecimal krwConvertedAmount, LocalDateTime createdAt) {
        return new EmergencyFunding(id, exchangeId, amount, krwConvertedAmount, createdAt);
    }
}
