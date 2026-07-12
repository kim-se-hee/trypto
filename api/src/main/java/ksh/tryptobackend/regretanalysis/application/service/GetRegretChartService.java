package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretChartUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretChartQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretChartResult;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.PortfolioQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AssetTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcDailyPrices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRegretChartService implements GetRegretChartUseCase {

    private final RegretReportQueryPort regretReportQueryPort;
    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final MarketDataQueryPort marketDataQueryPort;
    private final PortfolioQueryPort portfolioQueryPort;

    @Override
    @Transactional(readOnly = true)
    public RegretChartResult getRegretChart(GetRegretChartQuery query) {
        AnalysisRound round = investmentRoundQueryPort.getRound(query.roundId());
        round.validateOwnedBy(query.userId());

        RegretReport report = regretReportQueryPort.getByRoundIdAndExchangeId(query.roundId(), query.exchangeId());
        AnalysisExchange exchange = marketDataQueryPort.getExchange(query.exchangeId());
        AssetTimeline timeline = portfolioQueryPort.getAssetTimeline(query.roundId(), query.exchangeId());
        BtcDailyPrices btcDailyPrices = marketDataQueryPort.findBtcDailyPrices(
                timeline.getStartDate(), timeline.getEndDate(), exchange.currency());

        return RegretChartResult.from(
                query.roundId(),
                exchange,
                timeline,
                btcDailyPrices,
                report.getViolationDetails().toList());
    }
}
