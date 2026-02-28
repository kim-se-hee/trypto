package ksh.tryptobackend.ranking.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingStatsQuery;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;

public record GetRankingStatsRequest(
    @NotNull RankingPeriod period
) {
    public GetRankingStatsQuery toQuery() {
        return new GetRankingStatsQuery(period);
    }
}
