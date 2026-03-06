package ksh.tryptobackend.regretanalysis.domain.vo;

import java.time.LocalDateTime;

public record RuleViolation(
    Long violationId,
    Long orderId,
    Long ruleId,
    LocalDateTime createdAt
) {
}
