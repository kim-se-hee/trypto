package ksh.tryptobackend.ranking.domain.vo;

import java.math.BigDecimal;

public record RankingStats(
    long totalParticipants,
    BigDecimal maxProfitRate,
    BigDecimal avgProfitRate
) {
}
