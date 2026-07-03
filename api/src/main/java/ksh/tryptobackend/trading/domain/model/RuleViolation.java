package ksh.tryptobackend.trading.domain.model;

import java.time.LocalDateTime;
import lombok.Getter;

@Getter
public class RuleViolation {

    private final Long id;
    private final Long ruleId;
    private final String violationReason;
    private final LocalDateTime createdAt;

    private RuleViolation(Long id, Long ruleId, String violationReason, LocalDateTime createdAt) {
        this.id = id;
        this.ruleId = ruleId;
        this.violationReason = violationReason;
        this.createdAt = createdAt;
    }

    public static RuleViolation create(
            Long ruleId, String violationReason, LocalDateTime createdAt) {
        return new RuleViolation(null, ruleId, violationReason, createdAt);
    }

    public static RuleViolation reconstitute(
            Long id, Long ruleId, String violationReason, LocalDateTime createdAt) {
        return new RuleViolation(id, ruleId, violationReason, createdAt);
    }
}
