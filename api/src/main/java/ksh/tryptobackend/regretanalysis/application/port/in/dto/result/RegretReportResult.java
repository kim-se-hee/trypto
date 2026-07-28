package ksh.tryptobackend.regretanalysis.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.regretanalysis.domain.model.RoundRegretReport;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import ksh.tryptobackend.regretanalysis.domain.vo.RealizedLoss;
import ksh.tryptobackend.regretanalysis.domain.vo.RoundRuleImpact;
import ksh.tryptobackend.regretanalysis.domain.vo.ThresholdUnit;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolatedRule;
import ksh.tryptobackend.regretanalysis.domain.vo.ViolationRow;

public record RegretReportResult(
        Long roundId,
        int totalViolations,
        LocalDate analysisStart,
        LocalDate analysisEnd,
        BigDecimal totalViolationLoss,
        BigDecimal actualAsset,
        BigDecimal ruleFollowedAsset,
        List<RuleImpactResult> ruleImpacts,
        List<ViolationDetailResult> violationDetails) {

    public static RegretReportResult from(RoundRegretReport report, Map<Long, String> coinSymbols) {
        return new RegretReportResult(
                report.getRoundId(),
                report.getTotalViolations(),
                report.getAnalysisStart(),
                report.getAnalysisEnd(),
                report.getTotalViolationLoss(),
                report.getActualAsset(),
                report.getRuleFollowedAsset(),
                report.getRuleImpacts().stream()
                        .map(RegretReportResult::toRuleImpactResult)
                        .toList(),
                report.getViolationRows().stream()
                        .map(row -> toViolationDetailResult(row, coinSymbols))
                        .toList());
    }

    private static RuleImpactResult toRuleImpactResult(RoundRuleImpact impact) {
        return new RuleImpactResult(
                impact.rule().ruleId(),
                impact.rule().ruleType(),
                impact.rule().thresholdValue(),
                ThresholdUnit.from(impact.rule().ruleType()).symbol(),
                impact.violationCount(),
                impact.totalLossAmountKrw());
    }

    private static ViolationDetailResult toViolationDetailResult(ViolationRow row, Map<Long, String> coinSymbols) {
        AnalysisExchange exchange = row.exchange();
        return new ViolationDetailResult(
                row.violationDetailId(),
                row.orderId(),
                exchange.exchangeId(),
                exchange.name(),
                exchange.currency(),
                coinSymbols.getOrDefault(row.coinId(), ""),
                row.violatedRules().stream()
                        .map(RegretReportResult::toViolatedRuleResult)
                        .toList(),
                row.totalLossAmount(),
                row.totalLossAmountKrw(),
                row.occurredAt());
    }

    private static ViolatedRuleResult toViolatedRuleResult(ViolatedRule violatedRule) {
        return new ViolatedRuleResult(
                violatedRule.ruleType(),
                violatedRule.lossAmount(),
                violatedRule.lossAmountKrw(),
                violatedRule.krwLoss().unrealizedAmount(),
                violatedRule.krwLoss().realizedLosses().stream()
                        .map(RegretReportResult::toRealizationResult)
                        .toList());
    }

    private static RealizationResult toRealizationResult(RealizedLoss realizedLoss) {
        return new RealizationResult(realizedLoss.realizedOn(), realizedLoss.amount());
    }

    public record RuleImpactResult(
            Long ruleId,
            RuleType ruleType,
            BigDecimal thresholdValue,
            String thresholdUnit,
            int violationCount,
            BigDecimal totalLossAmount) {}

    public record RealizationResult(LocalDate realizedOn, BigDecimal lossAmountKrw) {}

    public record ViolatedRuleResult(
            RuleType ruleType,
            BigDecimal lossAmount,
            BigDecimal lossAmountKrw,
            BigDecimal unrealizedLossAmountKrw,
            List<RealizationResult> realizations) {}

    public record ViolationDetailResult(
            Long violationDetailId,
            Long orderId,
            Long exchangeId,
            String exchangeName,
            String currency,
            String coinSymbol,
            List<ViolatedRuleResult> violatedRules,
            BigDecimal totalLossAmount,
            BigDecimal totalLossAmountKrw,
            LocalDateTime occurredAt) {}
}
