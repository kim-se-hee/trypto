package ksh.tryptobackend.trading.domain.model;

import java.time.LocalDateTime;

public record RuleViolation(
    Long ruleId,
    String violationReason,
    LocalDateTime createdAt
) {
}
