package ksh.tryptobackend.regretanalysis.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;

public record GetRegretReportRequest(
    @NotNull Long userId,
    @NotNull Long exchangeId
) {

    public GetRegretReportQuery toQuery(Long roundId) {
        return new GetRegretReportQuery(userId, roundId, exchangeId);
    }
}
