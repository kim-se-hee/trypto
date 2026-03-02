package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.regretanalysis.application.port.in.GetRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GetRegretReportResult;
import ksh.tryptobackend.regretanalysis.application.port.out.*;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.*;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetails;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetRegretReportService implements GetRegretReportUseCase {

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
        validateWalletExistsForExchange(query.roundId(), query.exchangeId());
        ExchangeMetadata exchange = exchangeMetadataPort.getExchangeMetadata(query.exchangeId());
        List<RuleInfo> rules = investmentRulePort.findByRoundId(query.roundId());

        RegretReport report = findExistingOrGenerate(round, query.exchangeId(), rules);

        return toResult(report, exchange, rules);
    }

    private RoundInfoResult getRoundAndValidateOwner(Long roundId, Long userId) {
        RoundInfoResult round = investmentRoundPort.getRound(roundId);
        if (!round.userId().equals(userId)) {
            throw new CustomException(ErrorCode.ROUND_ACCESS_DENIED);
        }
        return round;
    }

    private void validateWalletExistsForExchange(Long roundId, Long exchangeId) {
        if (!exchangeMetadataPort.existsWalletForExchange(roundId, exchangeId)) {
            throw new CustomException(ErrorCode.EXCHANGE_NOT_FOUND);
        }
    }

    private RegretReport findExistingOrGenerate(RoundInfoResult round, Long exchangeId,
                                                 List<RuleInfo> rules) {
        return regretReportPersistencePort.findByRoundIdAndExchangeId(round.roundId(), exchangeId)
            .orElseGet(() -> generateAndSave(round, exchangeId, rules));
    }

    private RegretReport generateAndSave(RoundInfoResult round, Long exchangeId,
                                          List<RuleInfo> rules) {
        AssetSnapshot snapshot = getLatestSnapshot(round.roundId(), exchangeId);

        if (rules.isEmpty()) {
            return saveReport(RegretReport.createEmpty(
                round.userId(), round.roundId(), exchangeId,
                snapshot.totalProfitRate(),
                round.startedAt().toLocalDate(), snapshot.snapshotDate().toLocalDate()));
        }

        List<RuleViolationRecord> violations = fetchViolations(rules, exchangeId);
        if (violations.isEmpty()) {
            return saveReport(RegretReport.createEmpty(
                round.userId(), round.roundId(), exchangeId,
                snapshot.totalProfitRate(),
                round.startedAt().toLocalDate(), snapshot.snapshotDate().toLocalDate()));
        }

        List<ViolationDetail> violationDetailList = buildViolationDetails(violations, rules);
        ViolationDetails violationDetails = new ViolationDetails(violationDetailList);
        List<RuleImpact> ruleImpacts = violationDetails.toRuleImpacts(
            snapshot.totalAsset(), snapshot.totalInvestment(), snapshot.totalProfitRate());

        return saveReport(RegretReport.create(
            round.userId(), round.roundId(), exchangeId,
            violations.size(), snapshot.totalProfitRate(),
            snapshot.totalAsset(), snapshot.totalInvestment(),
            round.startedAt().toLocalDate(), snapshot.snapshotDate().toLocalDate(),
            ruleImpacts, violationDetailList));
    }

    private RegretReport saveReport(RegretReport report) {
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
            BigDecimal lossAmount = calculateLossAmount(rule, trade);
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

    private BigDecimal calculateLossAmount(RuleInfo rule, TradeRecord trade) {
        if (trade == null) return BigDecimal.ZERO;

        ViolationLossType lossType = ViolationLossType.from(rule.ruleType(), trade.side());
        List<TradeRecord> sellOrders = lossType == ViolationLossType.BUY_LOSS
            ? orderHistoryPort.findSellOrdersAfter(trade.walletId(), trade.exchangeCoinId(), trade.filledAt())
            : List.of();
        BigDecimal currentPrice = livePricePort.getCurrentPrice(trade.exchangeCoinId());

        return lossType.calculateLoss(trade, sellOrders, currentPrice);
    }

    private BigDecimal calculateProfitLoss(TradeRecord trade) {
        if (trade == null) return BigDecimal.ZERO;
        if (trade.side() == TradeSide.SELL) return trade.amount();

        BigDecimal currentPrice = livePricePort.getCurrentPrice(trade.exchangeCoinId());
        return currentPrice.multiply(trade.quantity()).subtract(trade.amount());
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

    private Map<Long, Long> resolveExchangeCoinIds(Map<Long, TradeRecord> tradeMap) {
        Set<Long> exchangeCoinIds = tradeMap.values().stream()
            .map(TradeRecord::exchangeCoinId)
            .collect(Collectors.toSet());
        return coinSymbolPort.findCoinIdsByExchangeCoinIds(exchangeCoinIds);
    }

    private GetRegretReportResult toResult(RegretReport report, ExchangeMetadata exchange,
                                            List<RuleInfo> rules) {
        Map<Long, RuleInfo> ruleMap = rules.stream()
            .collect(Collectors.toMap(RuleInfo::ruleId, r -> r));

        Set<Long> coinIds = report.getViolationDetails().stream()
            .map(ViolationDetail::getCoinId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        Map<Long, String> coinSymbols = coinSymbolPort.findSymbolsByIds(coinIds);

        return GetRegretReportResult.from(report, exchange, ruleMap, coinSymbols);
    }
}
