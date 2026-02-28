package ksh.tryptobackend.ranking.application.port.out.dto;

import java.math.BigDecimal;

public record RankingWithUserProjection(
    int rank,
    Long userId,
    String nickname,
    BigDecimal profitRate,
    int tradeCount,
    boolean portfolioPublic
) {
}
