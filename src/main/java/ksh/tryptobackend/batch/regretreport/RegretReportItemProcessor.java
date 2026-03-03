package ksh.tryptobackend.batch.regretreport;

import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRulePort;
import ksh.tryptobackend.regretanalysis.application.port.out.LivePricePort;
import ksh.tryptobackend.regretanalysis.application.port.out.OrderHistoryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.PortfolioSnapshotPort;
import ksh.tryptobackend.regretanalysis.application.port.out.RuleViolationPort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.RuleInfo;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.RuleViolationRecord;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.TradeRecord;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.TradeSide;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetails;
import ksh.tryptobackend.regretanalysis.domain.strategy.ViolationLossStrategy;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext.SoldPortion;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@StepScope
@RequiredArgsConstructor
public class RegretReportItemProcessor implements ItemProcessor<RegretReportInput, RegretReport> {

    private final InvestmentRulePort investmentRulePort;
    private final RuleViolationPort ruleViolationPort;
    private final OrderHistoryPort orderHistoryPort;
    private final LivePricePort livePricePort;
    private final PortfolioSnapshotPort portfolioSnapshotPort;

    private static final List<ViolationLossStrategy> LOSS_STRATEGIES = List.of(ViolationLossStrategy.values());

    @Override
    public RegretReport process(RegretReportInput input) {
        List<RuleInfo> rules = investmentRulePort.findByRoundId(input.roundId());
        if (rules.isEmpty()) {
            return null;
        }

        List<Long> ruleIds = rules.stream().map(RuleInfo::ruleId).toList();
        List<RuleViolationRecord> violations = ruleViolationPort.findByRuleIdsAndExchangeId(ruleIds, input.exchangeId());
        if (violations.isEmpty()) {
            return null;
        }

        Map<Long, RuleInfo> ruleMap = rules.stream()
            .collect(Collectors.toMap(RuleInfo::ruleId, r -> r));

        List<ViolationDetail> details = buildViolationDetails(violations, ruleMap);

        AssetSnapshot snapshot = portfolioSnapshotPort
            .findLatestByRoundIdAndExchangeId(input.roundId(), input.exchangeId())
            .orElse(null);
        BigDecimal actualProfitRate = snapshot != null ? snapshot.getTotalProfitRate() : BigDecimal.ZERO;
        BigDecimal totalInvestment = snapshot != null ? snapshot.getTotalInvestment() : BigDecimal.ZERO;

        ViolationDetails violationDetails = new ViolationDetails(details);
        List<RuleImpact> impacts = violationDetails.toRuleImpacts(totalInvestment);

        return RegretReport.generate(
            input.userId(), input.roundId(), input.exchangeId(),
            actualProfitRate, totalInvestment,
            impacts, details,
            input.startedAt().toLocalDate(), LocalDate.now()
        );
    }

    private List<ViolationDetail> buildViolationDetails(List<RuleViolationRecord> violations,
                                                         Map<Long, RuleInfo> ruleMap) {
        List<Long> orderIds = violations.stream()
            .map(RuleViolationRecord::orderId)
            .filter(Objects::nonNull)
            .toList();

        Map<Long, TradeRecord> tradeMap = orderHistoryPort.findByOrderIds(orderIds).stream()
            .collect(Collectors.toMap(TradeRecord::orderId, t -> t));

        Map<Long, BigDecimal> currentPrices = resolveCurrentPrices(tradeMap);

        List<ViolationDetail> details = new ArrayList<>();
        for (RuleViolationRecord violation : violations) {
            RuleInfo rule = ruleMap.get(violation.ruleId());
            if (rule == null || violation.orderId() == null) {
                continue;
            }

            TradeRecord trade = tradeMap.get(violation.orderId());
            if (trade == null) {
                continue;
            }

            ViolationLossContext context = buildLossContext(trade, currentPrices);
            ViolationLossStrategy strategy = resolveStrategy(rule.ruleType(), trade.side() == TradeSide.BUY);
            BigDecimal lossAmount = strategy.calculateLoss(context);

            details.add(ViolationDetail.create(
                violation.orderId(), violation.ruleId(), null,
                lossAmount, lossAmount, violation.createdAt()
            ));
        }
        return details;
    }

    private Map<Long, BigDecimal> resolveCurrentPrices(Map<Long, TradeRecord> tradeMap) {
        return tradeMap.values().stream()
            .map(TradeRecord::exchangeCoinId)
            .distinct()
            .collect(Collectors.toMap(id -> id, livePricePort::getCurrentPrice));
    }

    private ViolationLossContext buildLossContext(TradeRecord trade, Map<Long, BigDecimal> currentPrices) {
        BigDecimal currentPrice = currentPrices.get(trade.exchangeCoinId());
        List<SoldPortion> soldPortions = orderHistoryPort
            .findSellOrdersAfter(trade.walletId(), trade.exchangeCoinId(), trade.filledAt())
            .stream()
            .map(s -> new SoldPortion(s.filledPrice(), s.quantity()))
            .toList();
        return new ViolationLossContext(
            trade.filledPrice(), trade.quantity(), trade.amount(), currentPrice, soldPortions);
    }

    private ViolationLossStrategy resolveStrategy(RuleType ruleType, boolean isBuy) {
        return LOSS_STRATEGIES.stream()
            .filter(s -> s.supports(ruleType, isBuy))
            .findFirst()
            .orElseThrow();
    }
}
