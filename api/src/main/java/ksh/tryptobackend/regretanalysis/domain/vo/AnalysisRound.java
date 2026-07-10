package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public record AnalysisRound(
        Long roundId,
        Long userId,
        BigDecimal initialSeed,
        AnalysisRoundStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt) {

    public void validateOwnedBy(Long requesterId) {
        if (!userId.equals(requesterId)) {
            throw new CustomException(ErrorCode.ROUND_ACCESS_DENIED);
        }
    }

    public boolean isActive() {
        return status == AnalysisRoundStatus.ACTIVE;
    }

    public long getDurationDays() {
        LocalDateTime end = endedAt != null ? endedAt : LocalDateTime.now();
        return ChronoUnit.DAYS.between(startedAt, end);
    }
}
