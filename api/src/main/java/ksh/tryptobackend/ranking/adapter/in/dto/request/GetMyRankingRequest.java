package ksh.tryptobackend.ranking.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetMyRankingQuery;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;

public record GetMyRankingRequest(@NotNull RankingPeriod period) {

    public GetMyRankingQuery toQuery(Long userId) {
        return new GetMyRankingQuery(userId, period);
    }
}
