package ksh.tryptobackend.investmentround.application.service;

import ksh.tryptobackend.investmentround.application.port.in.GetRoundSummaryUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.GetRoundSummaryQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundSummaryResult;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRoundSummaryService implements GetRoundSummaryUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;

    @Override
    @Transactional(readOnly = true)
    public RoundSummaryResult getRoundSummary(GetRoundSummaryQuery query) {
        return new RoundSummaryResult(investmentRoundQueryPort.countByUserId(query.userId()));
    }
}
