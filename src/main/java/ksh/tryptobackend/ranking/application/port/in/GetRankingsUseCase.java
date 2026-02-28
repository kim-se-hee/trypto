package ksh.tryptobackend.ranking.application.port.in;

import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingsQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingCursorResult;

public interface GetRankingsUseCase {

    RankingCursorResult getRankings(GetRankingsQuery query);
}
