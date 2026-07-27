package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretChartUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretChartQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.PortfolioQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReports;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AssetTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.CapitalInflows;
import ksh.tryptobackend.regretanalysis.domain.vo.ExchangeCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRegretChartService implements GetRegretChartUseCase {

    private static final String CHART_CURRENCY = "KRW";

    private final RegretReportQueryPort regretReportQueryPort;
    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final MarketDataQueryPort marketDataQueryPort;
    private final PortfolioQueryPort portfolioQueryPort;

    @Override
    @Transactional(readOnly = true)
    public RegretChartResult getRegretChart(GetRegretChartQuery query) {
        AnalysisRound round = investmentRoundQueryPort.getRound(query.roundId());
        round.validateOwnedBy(query.userId());

        AssetTimeline timeline = portfolioQueryPort.getRoundAssetTimeline(query.roundId());
        if (timeline.isEmpty()) {
            return RegretChartResult.empty(query.roundId());
        }

        RegretReports reports = RegretReports.of(regretReportQueryPort.findAllByRoundId(query.roundId()));
        ExchangeCatalog exchanges = marketDataQueryPort.findExchanges(reports.extractExchangeIds());
        CapitalInflows capitalInflows = CapitalInflows.of(
                round.initialSeed(),
                investmentRoundQueryPort.findEmergencyCharges(query.roundId()),
                timeline.getStartDate());

        return RegretChartResult.from(
                query.roundId(),
                timeline,
                capitalInflows,
                marketDataQueryPort.findBtcDailyPrices(timeline.getStartDate(), timeline.getEndDate(), CHART_CURRENCY),
                reports.toViolationLosses(exchanges));
    }
}
