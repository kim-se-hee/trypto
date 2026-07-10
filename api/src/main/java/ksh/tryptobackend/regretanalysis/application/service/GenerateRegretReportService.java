package ksh.tryptobackend.regretanalysis.application.service;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.regretanalysis.application.port.in.GenerateRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.PortfolioQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.TradingQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolatedOrders;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetails;
import ksh.tryptobackend.regretanalysis.domain.vo.CurrentPrices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GenerateRegretReportService implements GenerateRegretReportUseCase {

    private final TradingQueryPort tradingQueryPort;
    private final MarketDataQueryPort marketDataQueryPort;
    private final PortfolioQueryPort portfolioQueryPort;
    private final Clock clock;

    @Override
    public Optional<RegretReport> generateReport(GenerateRegretReportCommand command) {
        ViolatedOrders violations =
                tradingQueryPort.findViolatedOrders(
                        command.roundId(), command.exchangeId(), command.walletId());
        if (violations.isEmpty()) {
            return Optional.empty();
        }

        CurrentPrices currentPrices =
                marketDataQueryPort.findCurrentPrices(violations.exchangeCoinIds());
        List<ViolationDetail> details = violations.calculateDetails(currentPrices);
        AssetSnapshot snapshot =
                portfolioQueryPort.getLatestSnapshot(command.roundId(), command.exchangeId());
        List<RuleImpact> impacts =
                new ViolationDetails(details).toRuleImpacts(snapshot.getTotalInvestment());

        return Optional.of(
                RegretReport.generate(
                        command.userId(),
                        command.roundId(),
                        command.exchangeId(),
                        snapshot,
                        impacts,
                        details,
                        command.startedAt().toLocalDate(),
                        clock));
    }
}
