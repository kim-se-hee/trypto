package ksh.tryptobackend.regretanalysis.application.port.out.dto;

import java.time.LocalDateTime;

public record RuleViolationRecord(
    Long violationId,
    Long orderId,
    Long ruleId,
    LocalDateTime createdAt
) {
}
