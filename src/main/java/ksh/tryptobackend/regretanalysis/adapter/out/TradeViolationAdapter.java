package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.regretanalysis.application.port.out.AnalysisRulePort;
import ksh.tryptobackend.regretanalysis.application.port.out.TradeRecordPort;
import ksh.tryptobackend.regretanalysis.application.port.out.RuleViolationPort;
import ksh.tryptobackend.regretanalysis.application.port.out.TradeViolationQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.TradeViolation;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRule;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRules;
import ksh.tryptobackend.regretanalysis.domain.vo.RuleViolation;
import ksh.tryptobackend.regretanalysis.domain.vo.TradeRecord;
import ksh.tryptobackend.regretanalysis.domain.vo.TradeSide;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationLossContext.SoldPortion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TradeViolationAdapter implements TradeViolationQueryPort {

    private final AnalysisRulePort analysisRulePort;
    private final RuleViolationPort ruleViolationPort;
    private final TradeRecordPort tradeRecordPort;

    @Override
    public List<TradeViolation> findByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        AnalysisRules rules = analysisRulePort.findByRoundId(roundId);
        if (rules.isEmpty()) {
            return List.of();
        }

        Map<Long, AnalysisRule> ruleMap = rules.toMap();

        List<RuleViolation> violations = ruleViolationPort
            .findByRuleIdsAndExchangeId(new ArrayList<>(ruleMap.keySet()), exchangeId);
        if (violations.isEmpty()) {
            return List.of();
        }

        Map<Long, TradeRecord> orderMap = loadOrderMap(violations);
        Map<SellOrderKey, List<TradeRecord>> sellOrderCache = loadSellOrders(orderMap);

        return violations.stream()
            .filter(v -> v.orderId() != null)
            .filter(v -> ruleMap.containsKey(v.ruleId()) && orderMap.containsKey(v.orderId()))
            .map(v -> toTradeViolation(v, ruleMap, orderMap, sellOrderCache))
            .toList();
    }

    private Map<Long, TradeRecord> loadOrderMap(List<RuleViolation> violations) {
        List<Long> orderIds = violations.stream()
            .map(RuleViolation::orderId)
            .filter(Objects::nonNull)
            .toList();
        return tradeRecordPort.findByOrderIds(orderIds).stream()
            .collect(Collectors.toMap(TradeRecord::orderId, t -> t));
    }

    private Map<SellOrderKey, List<TradeRecord>> loadSellOrders(Map<Long, TradeRecord> orderMap) {
        Map<SellOrderKey, List<TradeRecord>> buyOrderGroups = orderMap.values().stream()
            .filter(TradeRecord::isBuy)
            .collect(Collectors.groupingBy(o -> new SellOrderKey(o.walletId(), o.exchangeCoinId())));

        if (buyOrderGroups.isEmpty()) {
            return Map.of();
        }

        return buyOrderGroups.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> {
                    LocalDateTime earliestFilledAt = entry.getValue().stream()
                        .map(TradeRecord::filledAt)
                        .min(LocalDateTime::compareTo)
                        .orElseThrow(() -> new IllegalStateException(
                            "BUY 주문 그룹에 filledAt이 없습니다: key=" + entry.getKey()));
                    return tradeRecordPort.findSellOrdersAfter(
                        entry.getKey().walletId(), entry.getKey().exchangeCoinId(), earliestFilledAt);
                }
            ));
    }

    private TradeViolation toTradeViolation(RuleViolation violation,
                                             Map<Long, AnalysisRule> ruleMap,
                                             Map<Long, TradeRecord> orderMap,
                                             Map<SellOrderKey, List<TradeRecord>> sellOrderCache) {
        AnalysisRule rule = ruleMap.get(violation.ruleId());
        TradeRecord order = orderMap.get(violation.orderId());
        List<SoldPortion> soldPortions = resolveSoldPortions(order, sellOrderCache);

        return TradeViolation.create(
            order.orderId(), rule.ruleId(), rule.ruleType(),
            order.side(),
            order.filledPrice(), order.quantity(), order.amount(),
            order.exchangeCoinId(), violation.createdAt(),
            soldPortions
        );
    }

    private List<SoldPortion> resolveSoldPortions(TradeRecord order,
                                                    Map<SellOrderKey, List<TradeRecord>> sellOrderCache) {
        if (!order.isBuy()) {
            return List.of();
        }

        SellOrderKey key = new SellOrderKey(order.walletId(), order.exchangeCoinId());
        List<TradeRecord> sellOrders = sellOrderCache.getOrDefault(key, List.of());

        return sellOrders.stream()
            .filter(s -> s.filledAt().isAfter(order.filledAt()))
            .map(s -> new SoldPortion(s.filledPrice(), s.quantity()))
            .toList();
    }

    private record SellOrderKey(Long walletId, Long exchangeCoinId) {
    }
}
