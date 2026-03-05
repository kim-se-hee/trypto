package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.regretanalysis.application.port.in.GenerateRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.application.port.out.LivePricePort;
import ksh.tryptobackend.regretanalysis.application.port.out.PortfolioSnapshotPort;
import ksh.tryptobackend.regretanalysis.application.port.out.TradeViolationQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.TradeViolation;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenerateRegretReportService implements GenerateRegretReportUseCase {

    private final TradeViolationQueryPort tradeViolationQueryPort;
    private final LivePricePort livePricePort;
    private final PortfolioSnapshotPort portfolioSnapshotPort;

    @Override
    public Optional<RegretReport> generateReport(GenerateRegretReportCommand command) {
        List<TradeViolation> violations = findViolations(command);
        if (violations.isEmpty()) {
            return Optional.empty();
        }

        List<ViolationDetail> details = calculateViolationDetails(violations);
        AssetSnapshot snapshot = getLatestSnapshot(command);
        List<RuleImpact> impacts = new ViolationDetails(details).toRuleImpacts(snapshot.getTotalInvestment());

        return Optional.of(RegretReport.generate(
            command.userId(), command.roundId(), command.exchangeId(),
            snapshot.getTotalProfitRate(), snapshot.getTotalInvestment(),
            impacts, details,
            command.startedAt().toLocalDate(), LocalDate.now()
        ));
    }

    private List<TradeViolation> findViolations(GenerateRegretReportCommand command) {
        return tradeViolationQueryPort.findByRoundIdAndExchangeId(command.roundId(), command.exchangeId());
    }

    private List<ViolationDetail> calculateViolationDetails(List<TradeViolation> violations) {
        Map<Long, BigDecimal> currentPrices = resolveCurrentPrices(violations);
        return violations.stream()
            .map(v -> {
                BigDecimal lossAmount = v.calculateLoss(currentPrices.get(v.getExchangeCoinId()));
                return ViolationDetail.create(
                    v.getOrderId(), v.getRuleId(), v.getExchangeCoinId(),
                    lossAmount, lossAmount, v.getViolatedAt());
            })
            .toList();
    }

    private Map<Long, BigDecimal> resolveCurrentPrices(List<TradeViolation> violations) {
        return violations.stream()
            .map(TradeViolation::getExchangeCoinId)
            .distinct()
            .collect(Collectors.toMap(id -> id, livePricePort::getCurrentPrice));
    }

    private AssetSnapshot getLatestSnapshot(GenerateRegretReportCommand command) {
        return portfolioSnapshotPort.getLatestByRoundIdAndExchangeId(command.roundId(), command.exchangeId());
    }
}
