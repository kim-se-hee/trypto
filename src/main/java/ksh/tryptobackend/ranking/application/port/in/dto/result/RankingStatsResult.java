package ksh.tryptobackend.ranking.application.port.in.dto.result;

import java.math.BigDecimal;

public record RankingStatsResult(
    long totalParticipants,
    BigDecimal maxProfitRate,
    BigDecimal avgProfitRate
) {
}
