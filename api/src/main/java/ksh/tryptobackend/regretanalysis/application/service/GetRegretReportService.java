package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportResult;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReports;
import ksh.tryptobackend.regretanalysis.domain.model.RoundRegretReport;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRules;
import ksh.tryptobackend.regretanalysis.domain.vo.ExchangeCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRegretReportService implements GetRegretReportUseCase {

    private final RegretReportQueryPort regretReportQueryPort;
    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final MarketDataQueryPort marketDataQueryPort;

    @Override
    @Transactional(readOnly = true)
    public RegretReportResult getRegretReport(GetRegretReportQuery query) {
        AnalysisRound round = investmentRoundQueryPort.getRound(query.roundId());
        round.validateOwnedBy(query.userId());

        RegretReports reports = RegretReports.of(regretReportQueryPort.findAllByRoundId(query.roundId()));
        ExchangeCatalog exchanges = marketDataQueryPort.findExchanges(reports.extractExchangeIds());
        AnalysisRules rules = investmentRoundQueryPort.findRules(query.roundId());
        RoundRegretReport merged = reports.merge(query.roundId(), rules, exchanges);

        return RegretReportResult.from(merged, marketDataQueryPort.findCoinSymbols(reports.extractCoinIds()));
    }
}
