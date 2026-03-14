package ksh.tryptobackend.ranking.application.port.in.dto.result;

import java.math.BigDecimal;

public record RankingItemResult(
    int rank,
    Long userId,
    String nickname,
    BigDecimal profitRate,
    int tradeCount,
    boolean portfolioPublic
) {
}
