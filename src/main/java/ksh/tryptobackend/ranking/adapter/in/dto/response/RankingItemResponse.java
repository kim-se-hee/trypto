package ksh.tryptobackend.ranking.adapter.in.dto.response;

import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingItemResult;

import java.math.BigDecimal;

public record RankingItemResponse(
    int rank,
    Long userId,
    String nickname,
    BigDecimal profitRate,
    int tradeCount,
    boolean portfolioPublic
) {

    public static RankingItemResponse from(RankingItemResult result) {
        return new RankingItemResponse(
            result.rank(),
            result.userId(),
            result.nickname(),
            result.profitRate(),
            result.tradeCount(),
            result.portfolioPublic()
        );
    }
}
