package ksh.tryptobackend.ranking.application.port.in.dto.result;

import ksh.tryptobackend.ranking.domain.vo.ProfitRate;

public record MyRankingResult(
    int rank,
    String nickname,
    ProfitRate profitRate,
    int tradeCount
) {
}
