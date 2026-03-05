package ksh.tryptobackend.regretanalysis.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class RegretReport {

    private final Long reportId;
    private final Long userId;
    private final Long roundId;
    private final Long exchangeId;
    private final int totalViolations;
    private final BigDecimal missedProfit;
    private final BigDecimal actualProfitRate;
    private final BigDecimal ruleFollowedProfitRate;
    private final LocalDate analysisStart;
    private final LocalDate analysisEnd;
    private final LocalDateTime createdAt;
    private final List<RuleImpact> ruleImpacts;
    private final ViolationDetails violationDetails;

    private static final int RATE_SCALE = 4;

    public static RegretReport generate(Long userId, Long roundId, Long exchangeId,
                                        BigDecimal actualProfitRate, BigDecimal totalInvestment,
                                        List<RuleImpact> ruleImpacts,
                                        List<ViolationDetail> violationDetails,
                                        LocalDate analysisStart, LocalDate analysisEnd) {
        BigDecimal missedProfit = sumLossAmounts(violationDetails);
        BigDecimal ruleFollowedProfitRate = calculateRuleFollowedRate(
            actualProfitRate, missedProfit, totalInvestment);

        return RegretReport.builder()
            .userId(userId)
            .roundId(roundId)
            .exchangeId(exchangeId)
            .totalViolations(violationDetails.size())
            .missedProfit(missedProfit)
            .actualProfitRate(actualProfitRate)
            .ruleFollowedProfitRate(ruleFollowedProfitRate)
            .analysisStart(analysisStart)
            .analysisEnd(analysisEnd)
            .createdAt(LocalDateTime.now())
            .ruleImpacts(ruleImpacts)
            .violationDetails(new ViolationDetails(violationDetails))
            .build();
    }

    private static BigDecimal sumLossAmounts(List<ViolationDetail> violationDetails) {
        return violationDetails.stream()
            .map(ViolationDetail::getLossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal calculateRuleFollowedRate(BigDecimal actualProfitRate,
                                                        BigDecimal missedProfit,
                                                        BigDecimal totalInvestment) {
        if (totalInvestment.compareTo(BigDecimal.ZERO) == 0) {
            return actualProfitRate;
        }
        BigDecimal impactRate = missedProfit
            .divide(totalInvestment, RATE_SCALE, RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
        return actualProfitRate.add(impactRate);
    }

    public static RegretReport reconstitute(Long reportId, Long userId, Long roundId, Long exchangeId,
                                            int totalViolations, BigDecimal missedProfit,
                                            BigDecimal actualProfitRate, BigDecimal ruleFollowedProfitRate,
                                            LocalDate analysisStart, LocalDate analysisEnd,
                                            LocalDateTime createdAt,
                                            List<RuleImpact> ruleImpacts,
                                            List<ViolationDetail> violationDetails) {
        return RegretReport.builder()
            .reportId(reportId)
            .userId(userId)
            .roundId(roundId)
            .exchangeId(exchangeId)
            .totalViolations(totalViolations)
            .missedProfit(missedProfit)
            .actualProfitRate(actualProfitRate)
            .ruleFollowedProfitRate(ruleFollowedProfitRate)
            .analysisStart(analysisStart)
            .analysisEnd(analysisEnd)
            .createdAt(createdAt)
            .ruleImpacts(ruleImpacts)
            .violationDetails(new ViolationDetails(violationDetails))
            .build();
    }

}
