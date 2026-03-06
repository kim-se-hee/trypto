package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record AnalysisRound(
    Long roundId,
    Long userId,
    BigDecimal initialSeed,
    AnalysisRoundStatus status,
    LocalDateTime startedAt,
    LocalDateTime endedAt
) {

    public boolean isActive() {
        return status == AnalysisRoundStatus.ACTIVE;
    }

    public long getDurationDays() {
        LocalDateTime end = endedAt != null ? endedAt : LocalDateTime.now();
        return ChronoUnit.DAYS.between(startedAt, end);
    }
}
