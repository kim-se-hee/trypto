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

    private static final int RATE_SCALE = 4;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

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
    private final List<ViolationDetail> violationDetails;

    public static RegretReport create(Long userId, Long roundId, Long exchangeId,
                                       int totalViolations, BigDecimal actualProfitRate,
                                       BigDecimal actualTotalAsset, BigDecimal totalInvestment,
                                       LocalDate analysisStart, LocalDate analysisEnd,
                                       List<RuleImpact> ruleImpacts,
                                       List<ViolationDetail> violationDetails) {
        BigDecimal missedProfit = calculateMissedProfit(ruleImpacts);
        BigDecimal ruleFollowedProfitRate = calculateRuleFollowedProfitRate(
            actualTotalAsset, totalInvestment, ruleImpacts);

        return RegretReport.builder()
            .userId(userId)
            .roundId(roundId)
            .exchangeId(exchangeId)
            .totalViolations(totalViolations)
            .missedProfit(missedProfit)
            .actualProfitRate(actualProfitRate)
            .ruleFollowedProfitRate(ruleFollowedProfitRate)
            .analysisStart(analysisStart)
            .analysisEnd(analysisEnd)
            .createdAt(LocalDateTime.now())
            .ruleImpacts(ruleImpacts)
            .violationDetails(violationDetails)
            .build();
    }

    public static RegretReport createEmpty(Long userId, Long roundId, Long exchangeId,
                                           BigDecimal actualProfitRate,
                                           LocalDate analysisStart, LocalDate analysisEnd) {
        return RegretReport.builder()
            .userId(userId)
            .roundId(roundId)
            .exchangeId(exchangeId)
            .totalViolations(0)
            .missedProfit(BigDecimal.ZERO)
            .actualProfitRate(actualProfitRate)
            .ruleFollowedProfitRate(actualProfitRate)
            .analysisStart(analysisStart)
            .analysisEnd(analysisEnd)
            .createdAt(LocalDateTime.now())
            .ruleImpacts(List.of())
            .violationDetails(List.of())
            .build();
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
            .violationDetails(violationDetails)
            .build();
    }

    private static BigDecimal calculateMissedProfit(List<RuleImpact> ruleImpacts) {
        BigDecimal totalLoss = ruleImpacts.stream()
            .map(RuleImpact::getTotalLossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalLoss.compareTo(BigDecimal.ZERO) > 0 ? totalLoss : BigDecimal.ZERO;
    }

    private static BigDecimal calculateRuleFollowedProfitRate(BigDecimal actualTotalAsset,
                                                               BigDecimal totalInvestment,
                                                               List<RuleImpact> ruleImpacts) {
        BigDecimal totalLoss = ruleImpacts.stream()
            .map(RuleImpact::getTotalLossAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal ruleFollowedAsset = actualTotalAsset.add(totalLoss);
        return calculateProfitRate(ruleFollowedAsset, totalInvestment);
    }

    static BigDecimal calculateProfitRate(BigDecimal totalAsset, BigDecimal totalInvestment) {
        if (totalInvestment.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return totalAsset.subtract(totalInvestment)
            .divide(totalInvestment, RATE_SCALE, RoundingMode.HALF_UP)
            .multiply(HUNDRED);
    }
}
