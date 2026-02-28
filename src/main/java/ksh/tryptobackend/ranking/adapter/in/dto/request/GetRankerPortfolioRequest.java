package ksh.tryptobackend.ranking.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankerPortfolioQuery;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;

public record GetRankerPortfolioRequest(
    @NotNull RankingPeriod period
) {

    public GetRankerPortfolioQuery toQuery(Long userId) {
        return new GetRankerPortfolioQuery(userId, period);
    }
}
