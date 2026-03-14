package ksh.tryptobackend.investmentround.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoundOverview(
    Long roundId,
    Long userId,
    long roundNumber,
    BigDecimal initialSeed,
    BigDecimal emergencyFundingLimit,
    int emergencyChargeCount,
    RoundStatus status,
    LocalDateTime startedAt,
    LocalDateTime endedAt
) {
}
