package ksh.tryptobackend.investmentround.application.port.in;

import ksh.tryptobackend.investmentround.application.port.in.dto.query.GetRoundSummaryQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundSummaryResult;

public interface GetRoundSummaryUseCase {

    RoundSummaryResult getRoundSummary(GetRoundSummaryQuery query);
}
