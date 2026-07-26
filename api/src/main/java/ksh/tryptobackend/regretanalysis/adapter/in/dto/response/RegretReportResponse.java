package ksh.tryptobackend.regretanalysis.adapter.in.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportResult;

public record RegretReportResponse(
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
        List<RuleImpactResponse> ruleImpacts,
        List<ViolationDetailResponse> violationDetails) {

    public static RegretReportResponse from(RegretReportResult result) {
        List<RuleImpactResponse> ruleImpactResponses =
                result.ruleImpacts().stream().map(RuleImpactResponse::from).toList();

        List<ViolationDetailResponse> violationDetailResponses = result.violationDetails().stream()
                .map(ViolationDetailResponse::from)
                .toList();

        return new RegretReportResponse(
                result.reportId(),
                result.roundId(),
                result.exchangeId(),
                result.exchangeName(),
                result.currency(),
                result.totalViolations(),
                result.analysisStart(),
                result.analysisEnd(),
                result.missedProfit(),
                result.actualAsset(),
                result.ruleFollowedAsset(),
                ruleImpactResponses,
                violationDetailResponses);
    }

    public record RuleImpactResponse(
            Long ruleImpactId,
            Long ruleId,
            String ruleType,
            BigDecimal thresholdValue,
            String thresholdUnit,
            int violationCount,
            BigDecimal totalLossAmount) {

        public static RuleImpactResponse from(RegretReportResult.RuleImpactResult result) {
            return new RuleImpactResponse(
                    result.ruleImpactId(),
                    result.ruleId(),
                    result.ruleType() != null ? result.ruleType().name() : null,
                    result.thresholdValue(),
                    result.thresholdUnit(),
                    result.violationCount(),
                    result.totalLossAmount());
        }
    }

    public record ViolatedRuleResponse(String ruleType, BigDecimal lossAmount) {

        public static ViolatedRuleResponse from(RegretReportResult.ViolatedRuleResult result) {
            return new ViolatedRuleResponse(result.ruleType().name(), result.lossAmount());
        }
    }

    public record ViolationDetailResponse(
            Long violationDetailId,
            Long orderId,
            String coinSymbol,
            List<ViolatedRuleResponse> violatedRules,
            BigDecimal profitLoss,
            LocalDateTime occurredAt) {

        public static ViolationDetailResponse from(RegretReportResult.ViolationDetailResult result) {
            List<ViolatedRuleResponse> violatedRules = result.violatedRules().stream()
                    .map(ViolatedRuleResponse::from)
                    .toList();

            return new ViolationDetailResponse(
                    result.violationDetailId(),
                    result.orderId(),
                    result.coinSymbol(),
                    violatedRules,
                    result.profitLoss(),
                    result.occurredAt());
        }
    }
}
