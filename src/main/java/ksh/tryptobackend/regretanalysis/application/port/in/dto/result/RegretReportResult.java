package ksh.tryptobackend.regretanalysis.application.port.in.dto.result;

import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetails;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRule;
import ksh.tryptobackend.regretanalysis.domain.vo.ThresholdUnit;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    BigDecimal actualProfitRate,
    BigDecimal ruleFollowedProfitRate,
    List<RuleImpactResult> ruleImpacts,
    List<ViolationDetailResult> violationDetails
) {

    public static RegretReportResult from(RegretReport report,
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
            report.getActualProfitRate(),
            report.getRuleFollowedProfitRate(),
            toRuleImpactResults(report.getRuleImpacts(), ruleMap),
            toViolationDetailResults(report.getViolationDetails(), ruleMap, coinSymbols)
        );
    }

    private static List<RuleImpactResult> toRuleImpactResults(List<RuleImpact> ruleImpacts,
                                                               Map<Long, AnalysisRule> ruleMap) {
        return ruleImpacts.stream()
            .map(ri -> toRuleImpactResult(ri, ruleMap.get(ri.getRuleId())))
            .toList();
    }

    private static RuleImpactResult toRuleImpactResult(RuleImpact ruleImpact, AnalysisRule rule) {
        return new RuleImpactResult(
            ruleImpact.getRuleImpactId(),
            ruleImpact.getRuleId(),
            rule.ruleType(),
            rule.thresholdValue(),
            ThresholdUnit.from(rule.ruleType()).symbol(),
            ruleImpact.getViolationCount(),
            ruleImpact.getTotalLossAmount(),
            ruleImpact.getImpactGap().value()
        );
    }

    private static List<ViolationDetailResult> toViolationDetailResults(
            ViolationDetails violationDetails,
            Map<Long, AnalysisRule> ruleMap,
            Map<Long, String> coinSymbols) {
        List<ViolationDetailResult> results = new ArrayList<>();
        results.addAll(toOrderViolationResults(violationDetails, ruleMap, coinSymbols));
        results.addAll(toMonitoringViolationResults(violationDetails, ruleMap, coinSymbols));
        return results;
    }

    private static List<ViolationDetailResult> toOrderViolationResults(
            ViolationDetails violationDetails,
            Map<Long, AnalysisRule> ruleMap,
            Map<Long, String> coinSymbols) {
        return violationDetails.groupByOrder().entrySet().stream()
            .map(entry -> {
                ViolationDetail first = entry.getValue().getFirst();
                return new ViolationDetailResult(
                    first.getViolationDetailId(),
                    first.getOrderId(),
                    coinSymbols.getOrDefault(first.getCoinId(), ""),
                    extractViolatedRuleNames(entry.getValue(), ruleMap),
                    first.getProfitLoss(),
                    first.getOccurredAt());
            })
            .toList();
    }

    private static List<ViolationDetailResult> toMonitoringViolationResults(
            ViolationDetails violationDetails,
            Map<Long, AnalysisRule> ruleMap,
            Map<Long, String> coinSymbols) {
        return violationDetails.findMonitoringViolations().stream()
            .map(detail -> new ViolationDetailResult(
                detail.getViolationDetailId(),
                null,
                coinSymbols.getOrDefault(detail.getCoinId(), ""),
                List.of(ruleMap.get(detail.getRuleId()).ruleType().name()),
                detail.getProfitLoss(),
                detail.getOccurredAt()))
            .toList();
    }

    private static List<String> extractViolatedRuleNames(List<ViolationDetail> violations,
                                                          Map<Long, AnalysisRule> ruleMap) {
        return violations.stream()
            .map(d -> ruleMap.get(d.getRuleId()).ruleType().name())
            .distinct()
            .toList();
    }

    public record RuleImpactResult(
        Long ruleImpactId,
        Long ruleId,
        RuleType ruleType,
        BigDecimal thresholdValue,
        String thresholdUnit,
        int violationCount,
        BigDecimal totalLossAmount,
        BigDecimal impactGap
    ) {
    }

    public record ViolationDetailResult(
        Long violationDetailId,
        Long orderId,
        String coinSymbol,
        List<String> violatedRules,
        BigDecimal profitLoss,
        LocalDateTime occurredAt
    ) {
    }
}
