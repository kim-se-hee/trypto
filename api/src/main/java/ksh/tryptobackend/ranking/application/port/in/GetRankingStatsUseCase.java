package ksh.tryptobackend.ranking.application.port.in;

import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingStatsQuery;
import ksh.tryptobackend.ranking.domain.vo.RankingStats;

public interface GetRankingStatsUseCase {
    RankingStats getRankingStats(GetRankingStatsQuery query);
}
