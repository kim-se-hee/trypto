package ksh.tryptobackend.investmentround.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EmergencyFunding(Long id, Long exchangeId, BigDecimal amount, LocalDateTime createdAt) {

    public static EmergencyFunding create(Long exchangeId, BigDecimal amount, LocalDateTime createdAt) {
        return new EmergencyFunding(null, exchangeId, amount, createdAt);
    }

    public static EmergencyFunding reconstitute(Long id, Long exchangeId, BigDecimal amount, LocalDateTime createdAt) {
        return new EmergencyFunding(id, exchangeId, amount, createdAt);
    }
}
