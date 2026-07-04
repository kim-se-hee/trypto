package ksh.tryptobackend.investmentround.domain.vo;

import java.time.LocalDateTime;

public record DetectedViolation(Long ruleId, String violationReason, LocalDateTime createdAt) {}
