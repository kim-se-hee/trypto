package ksh.tryptobackend.regretanalysis.application.port.in.dto.result;

import ksh.tryptobackend.common.domain.vo.RuleType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GetRegretReportResult(
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
