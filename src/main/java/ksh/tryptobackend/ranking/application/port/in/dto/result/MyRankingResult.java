package ksh.tryptobackend.ranking.application.port.in.dto.result;

import ksh.tryptobackend.ranking.application.port.out.dto.RankingWithUserProjection;

import java.math.BigDecimal;

public record MyRankingResult(
    int rank,
    String nickname,
    BigDecimal profitRate,
    int tradeCount
) {

    public static MyRankingResult from(RankingWithUserProjection projection) {
        return new MyRankingResult(
            projection.rank(),
            projection.nickname(),
            projection.profitRate(),
            projection.tradeCount()
        );
    }
}
