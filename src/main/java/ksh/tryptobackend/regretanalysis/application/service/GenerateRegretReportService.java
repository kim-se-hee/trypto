package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindInvestmentRulesUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.InvestmentRuleResult;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInfoResult;
import ksh.tryptobackend.regretanalysis.application.port.in.GenerateRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolatedOrder;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetails;
import ksh.tryptobackend.regretanalysis.domain.vo.OrderExecution;
import ksh.tryptobackend.regretanalysis.domain.vo.RuleBreach;
import ksh.tryptobackend.regretanalysis.domain.vo.TradeSide;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext.SoldPortion;
import ksh.tryptobackend.trading.application.port.in.FindFilledOrdersUseCase;
import ksh.tryptobackend.trading.application.port.in.FindViolationsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.GetLivePriceUseCase;
import ksh.tryptobackend.trading.application.port.in.dto.result.FilledOrderResult;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GenerateRegretReportService implements GenerateRegretReportUseCase {

    private final FindInvestmentRulesUseCase findInvestmentRulesUseCase;
    private final FindViolationsUseCase findViolationsUseCase;
    private final FindFilledOrdersUseCase findFilledOrdersUseCase;
    private final GetLivePriceUseCase getLivePriceUseCase;
    private final FindSnapshotsUseCase findSnapshotsUseCase;
    private final Clock clock;

    @Override
    public Optional<RegretReport> generateReport(GenerateRegretReportCommand command) {
        List<ViolatedOrder> violations = findViolations(command);
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
            command.startedAt().toLocalDate(), LocalDate.now(clock),
            LocalDateTime.now(clock)
        ));
    }

    private List<ViolatedOrder> findViolations(GenerateRegretReportCommand command) {
        List<InvestmentRuleResult> rules = findInvestmentRulesUseCase.findByRoundId(command.roundId());
        if (rules.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> ruleIds = rules.stream().map(InvestmentRuleResult::ruleId).toList();
        Map<Long, InvestmentRuleResult> ruleMap = rules.stream()
            .collect(Collectors.toMap(InvestmentRuleResult::ruleId, r -> r));

        List<RuleBreach> breaches = findBreaches(ruleIds, command.exchangeId());
        if (breaches.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> orderIds = breaches.stream().map(RuleBreach::orderId).toList();
        Map<Long, OrderExecution> executionMap = findOrderExecutions(orderIds);

        return breaches.stream()
            .filter(breach -> executionMap.containsKey(breach.orderId()))
            .map(breach -> buildViolatedOrder(breach, ruleMap, executionMap, command.walletId()))
            .toList();
    }

    private List<RuleBreach> findBreaches(List<Long> ruleIds, Long exchangeId) {
        return findViolationsUseCase.findByRuleIdsAndExchangeId(ruleIds, exchangeId).stream()
            .map(r -> new RuleBreach(r.violationId(), r.orderId(), r.ruleId(), r.createdAt()))
            .toList();
    }

    private Map<Long, OrderExecution> findOrderExecutions(List<Long> orderIds) {
        return findFilledOrdersUseCase.findByOrderIds(orderIds).stream()
            .map(this::toOrderExecution)
            .collect(Collectors.toMap(OrderExecution::orderId, e -> e));
    }

    private OrderExecution toOrderExecution(FilledOrderResult result) {
        return new OrderExecution(
            result.orderId(), result.walletId(), result.exchangeCoinId(),
            TradeSide.valueOf(result.side()),
            result.amount(), result.quantity(), result.filledPrice(), result.filledAt());
    }

    private ViolatedOrder buildViolatedOrder(RuleBreach breach,
                                              Map<Long, InvestmentRuleResult> ruleMap,
                                              Map<Long, OrderExecution> executionMap,
                                              Long walletId) {
        InvestmentRuleResult rule = ruleMap.get(breach.ruleId());
        OrderExecution execution = executionMap.get(breach.orderId());
        List<SoldPortion> soldPortions = resolveSoldPortions(execution, walletId);

        return ViolatedOrder.create(
            execution.orderId(), rule.ruleId(), rule.ruleType(),
            execution.side(), execution.filledPrice(),
            execution.quantity(), execution.amount(),
            execution.exchangeCoinId(), breach.createdAt(),
            soldPortions);
    }

    private List<SoldPortion> resolveSoldPortions(OrderExecution execution, Long walletId) {
        if (execution.isSell()) {
            return Collections.emptyList();
        }
        return findFilledOrdersUseCase.findSellOrders(
                walletId, execution.exchangeCoinId(), execution.filledAt()).stream()
            .map(sell -> new SoldPortion(sell.filledPrice(), sell.quantity()))
            .toList();
    }

    private List<ViolationDetail> calculateViolationDetails(List<ViolatedOrder> violations) {
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

    private Map<Long, BigDecimal> resolveCurrentPrices(List<ViolatedOrder> violations) {
        return violations.stream()
            .map(ViolatedOrder::getExchangeCoinId)
            .distinct()
            .collect(Collectors.toMap(id -> id, getLivePriceUseCase::getCurrentPrice));
    }

    private AssetSnapshot getLatestSnapshot(GenerateRegretReportCommand command) {
        return findSnapshotsUseCase.findLatestByRoundIdAndExchangeId(
                command.roundId(), command.exchangeId())
            .map(this::toAssetSnapshot)
            .orElseThrow(() -> new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND));
    }

    private AssetSnapshot toAssetSnapshot(SnapshotInfoResult result) {
        return AssetSnapshot.reconstitute(
            result.snapshotId(), result.roundId(), result.exchangeId(),
            result.totalAsset(), result.totalInvestment(),
            result.totalProfitRate(), result.snapshotDate());
    }
}
