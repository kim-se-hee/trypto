package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.time.LocalDateTime;

public record ViolationResult(Long violationId, Long orderId, Long ruleId, LocalDateTime createdAt) {
}
