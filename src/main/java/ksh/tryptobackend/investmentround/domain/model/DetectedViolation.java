package ksh.tryptobackend.investmentround.domain.model;

import java.time.LocalDateTime;

public record DetectedViolation(Long ruleId, String violationReason, LocalDateTime createdAt) {
}
