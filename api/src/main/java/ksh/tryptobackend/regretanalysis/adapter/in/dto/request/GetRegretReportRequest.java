package ksh.tryptobackend.regretanalysis.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;

public record GetRegretReportRequest(@NotNull Long exchangeId) {

    public GetRegretReportQuery toQuery(Long roundId, Long userId) {
        return new GetRegretReportQuery(userId, roundId, exchangeId);
    }
}
