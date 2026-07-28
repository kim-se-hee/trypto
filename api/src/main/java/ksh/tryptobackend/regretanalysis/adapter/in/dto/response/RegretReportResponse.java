package ksh.tryptobackend.regretanalysis.adapter.in.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportResult;

public record RegretReportResponse(
        Long roundId,
        int totalViolations,
        LocalDate analysisStart,
        LocalDate analysisEnd,
        BigDecimal totalViolationLoss,
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
                result.roundId(),
                result.totalViolations(),
                result.analysisStart(),
                result.analysisEnd(),
                result.totalViolationLoss(),
                result.actualAsset(),
                result.ruleFollowedAsset(),
                ruleImpactResponses,
                violationDetailResponses);
    }

    public record RuleImpactResponse(
            Long ruleId,
            String ruleType,
            BigDecimal thresholdValue,
            String thresholdUnit,
            int violationCount,
            BigDecimal totalLossAmount) {

        public static RuleImpactResponse from(RegretReportResult.RuleImpactResult result) {
            return new RuleImpactResponse(
                    result.ruleId(),
                    result.ruleType() != null ? result.ruleType().name() : null,
                    result.thresholdValue(),
                    result.thresholdUnit(),
                    result.violationCount(),
                    result.totalLossAmount());
        }
    }

    /** 매도로 확정된 위반 손실 한 조각. 원칙 골라 보기가 이 날짜로 배수를 만든다. */
    public record RealizationResponse(LocalDate realizedOn, BigDecimal lossAmountKrw) {

        public static RealizationResponse from(RegretReportResult.RealizationResult result) {
            return new RealizationResponse(result.realizedOn(), result.lossAmountKrw());
        }
    }

    public record ViolatedRuleResponse(
            String ruleType,
            BigDecimal lossAmount,
            BigDecimal lossAmountKrw,
            BigDecimal unrealizedLossAmountKrw,
            List<RealizationResponse> realizations) {

        public static ViolatedRuleResponse from(RegretReportResult.ViolatedRuleResult result) {
            return new ViolatedRuleResponse(
                    result.ruleType().name(),
                    result.lossAmount(),
                    result.lossAmountKrw(),
                    result.unrealizedLossAmountKrw(),
                    result.realizations().stream()
                            .map(RealizationResponse::from)
                            .toList());
        }
    }

    public record ViolationDetailResponse(
            Long violationDetailId,
            Long orderId,
            Long exchangeId,
            String exchangeName,
            String currency,
            String coinSymbol,
            List<ViolatedRuleResponse> violatedRules,
            BigDecimal totalLossAmount,
            BigDecimal totalLossAmountKrw,
            LocalDateTime occurredAt) {

        public static ViolationDetailResponse from(RegretReportResult.ViolationDetailResult result) {
            List<ViolatedRuleResponse> violatedRules = result.violatedRules().stream()
                    .map(ViolatedRuleResponse::from)
                    .toList();

            return new ViolationDetailResponse(
                    result.violationDetailId(),
                    result.orderId(),
                    result.exchangeId(),
                    result.exchangeName(),
                    result.currency(),
                    result.coinSymbol(),
                    violatedRules,
                    result.totalLossAmount(),
                    result.totalLossAmountKrw(),
                    result.occurredAt());
        }
    }
}
