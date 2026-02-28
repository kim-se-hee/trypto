package ksh.tryptobackend.ranking.application.port.in.dto.query;

import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;

public record GetRankingStatsQuery(
    RankingPeriod period
) {
}
