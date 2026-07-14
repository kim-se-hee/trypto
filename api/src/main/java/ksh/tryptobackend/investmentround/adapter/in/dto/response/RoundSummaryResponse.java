package ksh.tryptobackend.investmentround.adapter.in.dto.response;

import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundSummaryResult;

public record RoundSummaryResponse(long totalRoundCount) {

    public static RoundSummaryResponse from(RoundSummaryResult result) {
        return new RoundSummaryResponse(result.totalRoundCount());
    }
}
