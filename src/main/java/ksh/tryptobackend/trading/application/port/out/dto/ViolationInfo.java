package ksh.tryptobackend.trading.application.port.out.dto;

import java.time.LocalDateTime;

public record ViolationInfo(
    Long violationId,
    Long orderId,
    Long ruleId,
    LocalDateTime createdAt
) {
}
