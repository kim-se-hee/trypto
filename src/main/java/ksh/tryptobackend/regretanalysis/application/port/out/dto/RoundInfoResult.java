package ksh.tryptobackend.regretanalysis.application.port.out.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoundInfoResult(
    Long roundId,
    Long userId,
    BigDecimal initialSeed,
    AnalysisRoundStatus status,
    LocalDateTime startedAt,
    LocalDateTime endedAt
) {
}
