package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GetRegretReportResult;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GetRegretReportResult.RuleImpactResult;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GetRegretReportResult.ViolationDetailResult;
import ksh.tryptobackend.regretanalysis.application.port.out.*;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.*;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.vo.ImpactGap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetRegretReportService implements GetRegretReportUseCase {

    private static final int RATE_SCALE = 4;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final InvestmentRoundPort investmentRoundPort;
    private final RegretReportPersistencePort regretReportPersistencePort;
    private final InvestmentRulePort investmentRulePort;
    private final RuleViolationPort ruleViolationPort;
    private final OrderHistoryPort orderHistoryPort;
    private final LivePricePort livePricePort;
    private final PortfolioSnapshotPort portfolioSnapshotPort;
    private final ExchangeMetadataPort exchangeMetadataPort;
    private final CoinSymbolPort coinSymbolPort;

    @Override
    @Transactional
    public GetRegretReportResult getRegretReport(GetRegretReportQuery query) {
        RoundInfoResult round = getRoundAndValidateOwner(query.roundId(), query.userId());
        validateExchangeInRound(query.roundId(), query.exchangeId());
        ExchangeMetadata exchange = exchangeMetadataPort.getExchangeMetadata(query.exchangeId());

        RegretReport report = findExistingOrGenerate(round, query.exchangeId());

        return toResult(report, exchange);
    }

    private RoundInfoResult getRoundAndValidateOwner(Long roundId, Long userId) {
        RoundInfoResult round = investmentRoundPort.getRound(roundId);
        if (!round.userId().equals(userId)) {
            throw new CustomException(ErrorCode.ROUND_ACCESS_DENIED);
        }
        return round;
    }

    private void validateExchangeInRound(Long roundId, Long exchangeId) {
        if (!exchangeMetadataPort.existsWalletForExchange(roundId, exchangeId)) {
            throw new CustomException(ErrorCode.EXCHANGE_NOT_FOUND);
        }
    }

    private RegretReport findExistingOrGenerate(RoundInfoResult round, Long exchangeId) {
        return regretReportPersistencePort.findByRoundIdAndExchangeId(round.roundId(), exchangeId)
            .orElseGet(() -> generateAndSave(round, exchangeId));
    }

    private RegretReport generateAndSave(RoundInfoResult round, Long exchangeId) {
        AssetSnapshot snapshot = getLatestSnapshot(round.roundId(), exchangeId);
        List<RuleInfo> rules = investmentRulePort.findByRoundId(round.roundId());

        if (rules.isEmpty()) {
            return saveEmptyReport(round, exchangeId, snapshot);
        }

        List<RuleViolationRecord> violations = fetchViolations(rules, exchangeId);
        if (violations.isEmpty()) {
            return saveEmptyReport(round, exchangeId, snapshot);
        }

        List<ViolationDetail> violationDetails = buildViolationDetails(violations, rules);
        List<RuleImpact> ruleImpacts = buildRuleImpacts(violationDetails, snapshot);

        RegretReport report = RegretReport.create(
            round.userId(), round.roundId(), exchangeId,
            violations.size(), snapshot.totalProfitRate(),
            snapshot.totalAsset(), snapshot.totalInvestment(),
            round.startedAt().toLocalDate(), snapshot.snapshotDate().toLocalDate(),
            ruleImpacts, violationDetails);

        return regretReportPersistencePort.save(report);
    }

    private RegretReport saveEmptyReport(RoundInfoResult round, Long exchangeId, AssetSnapshot snapshot) {
        RegretReport report = RegretReport.createEmpty(
            round.userId(), round.roundId(), exchangeId,
            snapshot.totalProfitRate(),
            round.startedAt().toLocalDate(), snapshot.snapshotDate().toLocalDate());

        return regretReportPersistencePort.save(report);
    }

    private AssetSnapshot getLatestSnapshot(Long roundId, Long exchangeId) {
        return portfolioSnapshotPort.findLatestByRoundIdAndExchangeId(roundId, exchangeId)
            .orElseThrow(() -> new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND));
    }

    private List<RuleViolationRecord> fetchViolations(List<RuleInfo> rules, Long exchangeId) {
        List<Long> ruleIds = rules.stream().map(RuleInfo::ruleId).toList();
        return ruleViolationPort.findByRuleIdsAndExchangeId(ruleIds, exchangeId);
    }

    private List<ViolationDetail> buildViolationDetails(List<RuleViolationRecord> violations,
                                                         List<RuleInfo> rules) {
        Map<Long, RuleInfo> ruleMap = rules.stream()
            .collect(Collectors.toMap(RuleInfo::ruleId, r -> r));
        Map<Long, TradeRecord> tradeMap = fetchTradeRecords(violations);
        Map<Long, Long> exchangeCoinIdToCoinId = resolveExchangeCoinIds(tradeMap);

        List<ViolationDetail> details = new ArrayList<>();
        for (RuleViolationRecord violation : violations) {
            RuleInfo rule = ruleMap.get(violation.ruleId());
            if (rule == null) continue;

            TradeRecord trade = violation.orderId() != null ? tradeMap.get(violation.orderId()) : null;
            BigDecimal lossAmount = calculateLossAmount(rule.ruleType(), trade);
            BigDecimal profitLoss = calculateProfitLoss(trade);
            LocalDateTime occurredAt = trade != null ? trade.filledAt() : violation.createdAt();
            Long coinId = trade != null
                ? exchangeCoinIdToCoinId.getOrDefault(trade.exchangeCoinId(), trade.exchangeCoinId())
                : null;

            details.add(ViolationDetail.create(
                violation.orderId(), violation.ruleId(), coinId,
                lossAmount, profitLoss, occurredAt));
        }
        return details;
    }

    private Map<Long, Long> resolveExchangeCoinIds(Map<Long, TradeRecord> tradeMap) {
        Set<Long> exchangeCoinIds = tradeMap.values().stream()
            .map(TradeRecord::exchangeCoinId)
            .collect(Collectors.toSet());
        return coinSymbolPort.findCoinIdsByExchangeCoinIds(exchangeCoinIds);
    }

    private Map<Long, TradeRecord> fetchTradeRecords(List<RuleViolationRecord> violations) {
        List<Long> orderIds = violations.stream()
            .map(RuleViolationRecord::orderId)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

        return orderHistoryPort.findByOrderIds(orderIds).stream()
            .collect(Collectors.toMap(TradeRecord::orderId, t -> t));
    }

    private BigDecimal calculateLossAmount(RuleType ruleType, TradeRecord trade) {
        if (trade == null) {
            return BigDecimal.ZERO;
        }
        return switch (ruleType) {
            case CHASE_BUY_BAN, AVERAGING_DOWN_LIMIT -> calculateBuyViolationLoss(trade);
            case OVERTRADING_LIMIT -> trade.side() == TradeSide.BUY
                ? calculateBuyViolationLoss(trade) : calculateSellViolationLoss(trade);
            case LOSS_CUT, PROFIT_TAKE -> BigDecimal.ZERO;
        };
    }

    private BigDecimal calculateBuyViolationLoss(TradeRecord trade) {
        List<TradeRecord> sellOrders = orderHistoryPort.findSellOrdersAfter(
            trade.walletId(), trade.exchangeCoinId(), trade.filledAt());

        BigDecimal remainingQty = trade.quantity();
        BigDecimal realizedLoss = BigDecimal.ZERO;

        for (TradeRecord sell : sellOrders) {
            if (remainingQty.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal matchQty = sell.quantity().min(remainingQty);
            realizedLoss = realizedLoss.add(
                trade.filledPrice().subtract(sell.filledPrice()).multiply(matchQty));
            remainingQty = remainingQty.subtract(matchQty);
        }

        if (remainingQty.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentPrice = livePricePort.getCurrentPrice(trade.exchangeCoinId());
            realizedLoss = realizedLoss.add(
                trade.filledPrice().subtract(currentPrice).multiply(remainingQty));
        }

        return realizedLoss;
    }

    private BigDecimal calculateSellViolationLoss(TradeRecord trade) {
        BigDecimal currentPrice = livePricePort.getCurrentPrice(trade.exchangeCoinId());
        return currentPrice.subtract(trade.filledPrice()).multiply(trade.quantity());
    }

    private BigDecimal calculateProfitLoss(TradeRecord trade) {
        if (trade == null) return BigDecimal.ZERO;

        if (trade.side() == TradeSide.SELL) {
            return trade.amount();
        }
        BigDecimal currentPrice = livePricePort.getCurrentPrice(trade.exchangeCoinId());
        return currentPrice.multiply(trade.quantity()).subtract(trade.amount());
    }

    private List<RuleImpact> buildRuleImpacts(List<ViolationDetail> details, AssetSnapshot snapshot) {
        Map<Long, List<ViolationDetail>> detailsByRule = details.stream()
            .collect(Collectors.groupingBy(ViolationDetail::getRuleId));

        BigDecimal totalInvestment = snapshot.totalInvestment();
        BigDecimal actualTotalAsset = snapshot.totalAsset();
        BigDecimal actualProfitRate = snapshot.totalProfitRate();

        List<RuleImpact> impacts = new ArrayList<>();
        for (Map.Entry<Long, List<ViolationDetail>> entry : detailsByRule.entrySet()) {
            Long ruleId = entry.getKey();
            List<ViolationDetail> ruleDetails = entry.getValue();

            BigDecimal totalLossAmount = ruleDetails.stream()
                .map(ViolationDetail::getLossAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

            BigDecimal ruleFollowedAsset = actualTotalAsset.add(totalLossAmount);
            BigDecimal ruleFollowedRate = calculateProfitRate(ruleFollowedAsset, totalInvestment);
            BigDecimal gap = ruleFollowedRate.subtract(actualProfitRate);

            impacts.add(RuleImpact.create(ruleId, ruleDetails.size(), totalLossAmount, ImpactGap.of(gap)));
        }

        return impacts;
    }

    private BigDecimal calculateProfitRate(BigDecimal totalAsset, BigDecimal totalInvestment) {
        if (totalInvestment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalAsset.subtract(totalInvestment)
            .divide(totalInvestment, RATE_SCALE, RoundingMode.HALF_UP)
            .multiply(HUNDRED);
    }

    private GetRegretReportResult toResult(RegretReport report, ExchangeMetadata exchange) {
        Map<Long, RuleInfo> ruleMap = investmentRulePort.findByRoundId(report.getRoundId()).stream()
            .collect(Collectors.toMap(RuleInfo::ruleId, r -> r));

        Set<Long> coinIds = report.getViolationDetails().stream()
            .map(ViolationDetail::getCoinId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> coinSymbols = coinSymbolPort.findSymbolsByIds(coinIds);

        List<RuleImpactResult> ruleImpactResults = report.getRuleImpacts().stream()
            .map(ri -> toRuleImpactResult(ri, ruleMap))
            .toList();

        List<ViolationDetailResult> violationDetailResults = groupViolationDetails(
            report.getViolationDetails(), ruleMap, coinSymbols);

        return new GetRegretReportResult(
            report.getReportId(),
            report.getRoundId(),
            report.getExchangeId(),
            exchange.name(),
            exchange.currency(),
            report.getTotalViolations(),
            report.getAnalysisStart(),
            report.getAnalysisEnd(),
            report.getMissedProfit(),
            report.getActualProfitRate(),
            report.getRuleFollowedProfitRate(),
            ruleImpactResults,
            violationDetailResults
        );
    }

    private RuleImpactResult toRuleImpactResult(RuleImpact ruleImpact, Map<Long, RuleInfo> ruleMap) {
        RuleInfo rule = ruleMap.get(ruleImpact.getRuleId());
        RuleType ruleType = rule != null ? rule.ruleType() : null;
        BigDecimal thresholdValue = rule != null ? rule.thresholdValue() : BigDecimal.ZERO;

        return new RuleImpactResult(
            ruleImpact.getRuleImpactId(),
            ruleImpact.getRuleId(),
            ruleType,
            thresholdValue,
            resolveThresholdUnit(ruleType),
            ruleImpact.getViolationCount(),
            ruleImpact.getTotalLossAmount(),
            ruleImpact.getImpactGap().value()
        );
    }

    private String resolveThresholdUnit(RuleType ruleType) {
        if (ruleType == null) return "";
        return switch (ruleType) {
            case LOSS_CUT, PROFIT_TAKE, CHASE_BUY_BAN -> "%";
            case AVERAGING_DOWN_LIMIT, OVERTRADING_LIMIT -> "회";
        };
    }

    private List<ViolationDetailResult> groupViolationDetails(List<ViolationDetail> details,
                                                                Map<Long, RuleInfo> ruleMap,
                                                                Map<Long, String> coinSymbols) {
        Map<Long, List<ViolationDetail>> orderGroup = new LinkedHashMap<>();
        List<ViolationDetail> monitoringViolations = new ArrayList<>();

        for (ViolationDetail detail : details) {
            if (detail.getOrderId() != null) {
                orderGroup.computeIfAbsent(detail.getOrderId(), k -> new ArrayList<>()).add(detail);
            } else {
                monitoringViolations.add(detail);
            }
        }

        List<ViolationDetailResult> results = new ArrayList<>();

        for (Map.Entry<Long, List<ViolationDetail>> entry : orderGroup.entrySet()) {
            List<ViolationDetail> grouped = entry.getValue();
            ViolationDetail first = grouped.getFirst();

            List<String> violatedRules = grouped.stream()
                .map(d -> ruleMap.get(d.getRuleId()))
                .filter(Objects::nonNull)
                .map(r -> r.ruleType().name())
                .distinct()
                .toList();

            String coinSymbol = coinSymbols.getOrDefault(first.getCoinId(), "");

            results.add(new ViolationDetailResult(
                first.getViolationDetailId(), first.getOrderId(), coinSymbol,
                violatedRules, first.getProfitLoss(), first.getOccurredAt()));
        }

        for (ViolationDetail detail : monitoringViolations) {
            RuleInfo rule = ruleMap.get(detail.getRuleId());
            String ruleName = rule != null ? rule.ruleType().name() : "";
            String coinSymbol = coinSymbols.getOrDefault(detail.getCoinId(), "");

            results.add(new ViolationDetailResult(
                detail.getViolationDetailId(), null, coinSymbol,
                List.of(ruleName), detail.getProfitLoss(), detail.getOccurredAt()));
        }

        return results;
    }
}
