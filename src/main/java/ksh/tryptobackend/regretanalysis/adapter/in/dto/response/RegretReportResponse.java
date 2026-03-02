package ksh.tryptobackend.regretanalysis.adapter.in.dto.response;

import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
    BigDecimal actualProfitRate,
    BigDecimal ruleFollowedProfitRate,
    List<RuleImpactResponse> ruleImpacts,
    List<ViolationDetailResponse> violationDetails
) {

    public static RegretReportResponse from(RegretReportResult result) {
        List<RuleImpactResponse> ruleImpactResponses = result.ruleImpacts().stream()
            .map(RuleImpactResponse::from)
            .toList();

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
            result.actualProfitRate(),
            result.ruleFollowedProfitRate(),
            ruleImpactResponses,
            violationDetailResponses
        );
    }

    public record RuleImpactResponse(
        Long ruleImpactId,
        Long ruleId,
        String ruleType,
        BigDecimal thresholdValue,
        String thresholdUnit,
        int violationCount,
        BigDecimal totalLossAmount,
        BigDecimal impactGap
    ) {

        public static RuleImpactResponse from(RegretReportResult.RuleImpactResult result) {
            return new RuleImpactResponse(
                result.ruleImpactId(),
                result.ruleId(),
                result.ruleType() != null ? result.ruleType().name() : null,
                result.thresholdValue(),
                result.thresholdUnit(),
                result.violationCount(),
                result.totalLossAmount(),
                result.impactGap()
            );
        }
    }

    public record ViolationDetailResponse(
        Long violationDetailId,
        Long orderId,
        String coinSymbol,
        List<String> violatedRules,
        BigDecimal profitLoss,
        LocalDateTime occurredAt
    ) {

        public static ViolationDetailResponse from(RegretReportResult.ViolationDetailResult result) {
            return new ViolationDetailResponse(
                result.violationDetailId(),
                result.orderId(),
                result.coinSymbol(),
                result.violatedRules(),
                result.profitLoss(),
                result.occurredAt()
            );
        }
    }
}
