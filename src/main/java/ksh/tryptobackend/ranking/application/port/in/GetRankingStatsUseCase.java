package ksh.tryptobackend.ranking.application.port.in;

import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingStatsQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingStatsResult;

public interface GetRankingStatsUseCase {
    RankingStatsResult getRankingStats(GetRankingStatsQuery query);
}
