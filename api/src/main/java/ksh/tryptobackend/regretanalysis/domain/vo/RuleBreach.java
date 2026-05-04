package ksh.tryptobackend.regretanalysis.domain.vo;

import java.time.LocalDateTime;

public record RuleBreach(Long violationId, Long orderId, Long ruleId, LocalDateTime createdAt) {}
