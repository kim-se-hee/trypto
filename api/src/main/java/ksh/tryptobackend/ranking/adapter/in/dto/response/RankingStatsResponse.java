package ksh.tryptobackend.ranking.adapter.in.dto.response;

import java.math.BigDecimal;
import ksh.tryptobackend.ranking.domain.vo.RankingStats;

public record RankingStatsResponse(
        long totalParticipants, BigDecimal maxProfitRate, BigDecimal avgProfitRate) {
    public static RankingStatsResponse from(RankingStats stats) {
        return new RankingStatsResponse(
                stats.totalParticipants(), stats.maxProfitRate(), stats.avgProfitRate());
    }
}
