package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.investmentround.domain.vo.DetectedViolation;

public record RuleViolationCheckResult(
        Long ruleId, String violationReason, LocalDateTime createdAt) {

    public static List<RuleViolationCheckResult> from(List<DetectedViolation> violations) {
        return violations.stream().map(RuleViolationCheckResult::from).toList();
    }

    public static RuleViolationCheckResult from(DetectedViolation violation) {
        return new RuleViolationCheckResult(
                violation.ruleId(), violation.violationReason(), violation.createdAt());
    }
}
