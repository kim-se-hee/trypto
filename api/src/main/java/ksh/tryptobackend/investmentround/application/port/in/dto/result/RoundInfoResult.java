package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.investmentround.domain.vo.RoundOverview;

public record RoundInfoResult(
        Long roundId,
        Long userId,
        long roundNumber,
        BigDecimal initialSeed,
        BigDecimal emergencyFundingLimit,
        int emergencyChargeCount,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt) {

    public static RoundInfoResult from(RoundOverview info) {
        return new RoundInfoResult(
                info.roundId(),
                info.userId(),
                info.roundNumber(),
                info.initialSeed(),
                info.emergencyFundingLimit(),
                info.emergencyChargeCount(),
                info.status().name(),
                info.startedAt(),
                info.endedAt());
    }
}
