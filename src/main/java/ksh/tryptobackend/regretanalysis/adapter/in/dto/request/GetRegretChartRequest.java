package ksh.tryptobackend.regretanalysis.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretChartQuery;

public record GetRegretChartRequest(
    @NotNull Long exchangeId,
    @NotNull Long userId
) {

    public GetRegretChartQuery toQuery(Long roundId) {
        return new GetRegretChartQuery(roundId, exchangeId, userId);
    }
}
