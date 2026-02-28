package ksh.tryptobackend.ranking.application.port.out.dto;

import java.math.BigDecimal;

public record RankingStatsProjection(
    long totalParticipants,
    BigDecimal maxProfitRate,
    BigDecimal avgProfitRate
) {
}
