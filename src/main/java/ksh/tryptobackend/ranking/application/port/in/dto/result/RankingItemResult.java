package ksh.tryptobackend.ranking.application.port.in.dto.result;

import ksh.tryptobackend.ranking.application.port.out.dto.RankingWithUserProjection;

import java.math.BigDecimal;

public record RankingItemResult(
    int rank,
    Long userId,
    String nickname,
    BigDecimal profitRate,
    int tradeCount,
    boolean portfolioPublic
) {

    public static RankingItemResult from(RankingWithUserProjection projection) {
        return new RankingItemResult(
            projection.rank(),
            projection.userId(),
            projection.nickname(),
            projection.profitRate(),
            projection.tradeCount(),
            projection.portfolioPublic()
        );
    }
}
