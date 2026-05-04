package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.time.LocalDateTime;
import ksh.tryptobackend.investmentround.domain.model.DetectedViolation;

public record RuleViolationResult(Long ruleId, String violationReason, LocalDateTime createdAt) {

    public static RuleViolationResult from(DetectedViolation violation) {
        return new RuleViolationResult(
                violation.ruleId(), violation.violationReason(), violation.createdAt());
    }
}
