package ksh.tryptobackend.regretanalysis.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetails;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRule;
import ksh.tryptobackend.regretanalysis.domain.vo.ThresholdUnit;

public record RegretReportResult(
        Long reportId,
        Long roundId,
        Long exchangeId,
        String exchangeName,
        String currency,
        int totalViolations,
        LocalDate analysisStart,
        LocalDate analysisEnd,
        BigDecimal missedProfit,
        BigDecimal actualAsset,
        BigDecimal ruleFollowedAsset,
        List<RuleImpactResult> ruleImpacts,
        List<ViolationDetailResult> violationDetails) {

    public static RegretReportResult from(
            RegretReport report,
            AnalysisExchange exchange,
            Map<Long, AnalysisRule> ruleMap,
            Map<Long, String> coinSymbols) {
        return new RegretReportResult(
                report.getReportId(),
                report.getRoundId(),
                report.getExchangeId(),
                exchange.name(),
                exchange.currency(),
                report.getTotalViolations(),
                report.getAnalysisStart(),
                report.getAnalysisEnd(),
                report.getMissedProfit(),
                report.getActualAsset(),
                report.getRuleFollowedAsset(),
                toRuleImpactResults(report.getRuleImpacts(), ruleMap),
                toViolationDetailResults(report.getViolationDetails(), ruleMap, coinSymbols));
    }

    private static List<RuleImpactResult> toRuleImpactResults(
            List<RuleImpact> ruleImpacts, Map<Long, AnalysisRule> ruleMap) {
        Map<Long, RuleImpact> impactByRuleId =
                ruleImpacts.stream().collect(Collectors.toMap(RuleImpact::getRuleId, impact -> impact));

        return ruleMap.values().stream()
                .sorted(Comparator.comparing(AnalysisRule::ruleId))
                .map(rule -> toRuleImpactResult(rule, impactByRuleId.get(rule.ruleId())))
                .toList();
    }

    private static RuleImpactResult toRuleImpactResult(AnalysisRule rule, RuleImpact ruleImpact) {
        if (ruleImpact == null) {
            return new RuleImpactResult(
                    null,
                    rule.ruleId(),
                    rule.ruleType(),
                    rule.thresholdValue(),
                    ThresholdUnit.from(rule.ruleType()).symbol(),
                    0,
                    BigDecimal.ZERO);
        }

        return new RuleImpactResult(
                ruleImpact.getRuleImpactId(),
                rule.ruleId(),
                rule.ruleType(),
                rule.thresholdValue(),
                ThresholdUnit.from(rule.ruleType()).symbol(),
                ruleImpact.getViolationCount(),
                ruleImpact.getTotalLossAmount());
    }

    private static List<ViolationDetailResult> toViolationDetailResults(
            ViolationDetails violationDetails, Map<Long, AnalysisRule> ruleMap, Map<Long, String> coinSymbols) {
        List<ViolationDetailResult> results = new ArrayList<>();
        results.addAll(toOrderViolationResults(violationDetails, ruleMap, coinSymbols));
        results.addAll(toMonitoringViolationResults(violationDetails, ruleMap, coinSymbols));
        return results;
    }

    private static List<ViolationDetailResult> toOrderViolationResults(
            ViolationDetails violationDetails, Map<Long, AnalysisRule> ruleMap, Map<Long, String> coinSymbols) {
        return violationDetails.groupByOrder().entrySet().stream()
                .map(entry -> {
                    ViolationDetail first = entry.getValue().getFirst();
                    return new ViolationDetailResult(
                            first.getViolationDetailId(),
                            first.getOrderId(),
                            coinSymbols.getOrDefault(first.getCoinId(), ""),
                            extractViolatedRules(entry.getValue(), ruleMap),
                            sumProfitLoss(entry.getValue()),
                            first.getOccurredAt());
                })
                .toList();
    }

    private static List<ViolationDetailResult> toMonitoringViolationResults(
            ViolationDetails violationDetails, Map<Long, AnalysisRule> ruleMap, Map<Long, String> coinSymbols) {
        return violationDetails.findMonitoringViolations().stream()
                .map(detail -> new ViolationDetailResult(
                        detail.getViolationDetailId(),
                        null,
                        coinSymbols.getOrDefault(detail.getCoinId(), ""),
                        List.of(new ViolatedRuleResult(
                                ruleMap.get(detail.getRuleId()).ruleType(), detail.getLossAmount())),
                        detail.getProfitLoss(),
                        detail.getOccurredAt()))
                .toList();
    }

    /**
     * 하나의 주문이 같은 원칙을 여러 번 어겼다면 위반 손익을 합쳐 하나로 만든다. 복기 그래프가 원칙별로 곡선을 되돌릴 때 이 금액을 사용한다.
     */
    private static List<ViolatedRuleResult> extractViolatedRules(
            List<ViolationDetail> violations, Map<Long, AnalysisRule> ruleMap) {
        Map<RuleType, BigDecimal> lossByRuleType = new LinkedHashMap<>();
        for (ViolationDetail violation : violations) {
            RuleType ruleType = ruleMap.get(violation.getRuleId()).ruleType();
            lossByRuleType.merge(ruleType, violation.getLossAmount(), BigDecimal::add);
        }

        return lossByRuleType.entrySet().stream()
                .map(entry -> new ViolatedRuleResult(entry.getKey(), entry.getValue()))
                .toList();
    }

    private static BigDecimal sumProfitLoss(List<ViolationDetail> violations) {
        return violations.stream().map(ViolationDetail::getProfitLoss).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record RuleImpactResult(
            Long ruleImpactId,
            Long ruleId,
            RuleType ruleType,
            BigDecimal thresholdValue,
            String thresholdUnit,
            int violationCount,
            BigDecimal totalLossAmount) {}

    public record ViolatedRuleResult(RuleType ruleType, BigDecimal lossAmount) {}

    public record ViolationDetailResult(
            Long violationDetailId,
            Long orderId,
            String coinSymbol,
            List<ViolatedRuleResult> violatedRules,
            BigDecimal profitLoss,
            LocalDateTime occurredAt) {}
}
