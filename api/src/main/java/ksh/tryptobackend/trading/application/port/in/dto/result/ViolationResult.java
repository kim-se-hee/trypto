package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.time.LocalDateTime;
import ksh.tryptobackend.trading.domain.vo.RuleViolationRef;

public record ViolationResult(Long violationId, Long orderId, Long ruleId, LocalDateTime createdAt) {

    public static ViolationResult from(RuleViolationRef ref) {
        return new ViolationResult(ref.violationId(), ref.orderId(), ref.ruleId(), ref.createdAt());
    }
}
