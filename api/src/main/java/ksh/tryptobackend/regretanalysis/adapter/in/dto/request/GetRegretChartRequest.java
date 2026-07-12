package ksh.tryptobackend.regretanalysis.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretChartQuery;

public record GetRegretChartRequest(@NotNull Long exchangeId) {

    public GetRegretChartQuery toQuery(Long roundId, Long userId) {
        return new GetRegretChartQuery(roundId, exchangeId, userId);
    }
}
