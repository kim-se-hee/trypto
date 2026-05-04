package ksh.tryptobackend.ranking.adapter.in.dto.response;

import java.math.BigDecimal;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingStatsResult;

public record RankingStatsResponse(
        long totalParticipants, BigDecimal maxProfitRate, BigDecimal avgProfitRate) {
    public static RankingStatsResponse from(RankingStatsResult result) {
        return new RankingStatsResponse(
                result.totalParticipants(), result.maxProfitRate(), result.avgProfitRate());
    }
}
