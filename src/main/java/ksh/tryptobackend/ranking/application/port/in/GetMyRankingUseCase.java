package ksh.tryptobackend.ranking.application.port.in;

import ksh.tryptobackend.ranking.application.port.in.dto.query.GetMyRankingQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.MyRankingResult;

public interface GetMyRankingUseCase {

    MyRankingResult getMyRanking(GetMyRankingQuery query);
}
