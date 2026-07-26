package ksh.tryptobackend.regretanalysis.domain.model;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegretReport {

    private final Long reportId;
    private final Long userId;
    private final Long roundId;
    private final Long exchangeId;
    private final int totalViolations;
    private final BigDecimal missedProfit;
    private final BigDecimal actualAsset;
    private final BigDecimal ruleFollowedAsset;
    private final LocalDate analysisStart;
    private final LocalDate analysisEnd;
    private final LocalDateTime createdAt;
    private final List<RuleImpact> ruleImpacts;
    private final ViolationDetails violationDetails;

    public static RegretReport generate(
            Long userId,
            Long roundId,
            Long exchangeId,
            AssetSnapshot snapshot,
            List<RuleImpact> ruleImpacts,
            List<ViolationDetail> violationDetails,
            LocalDate analysisStart,
            Clock clock) {
        BigDecimal actualAsset = snapshot.getTotalAsset();
        BigDecimal missedProfit = sumLossAmounts(violationDetails);

        return RegretReport.builder()
                .userId(userId)
                .roundId(roundId)
                .exchangeId(exchangeId)
                .totalViolations(violationDetails.size())
                .missedProfit(missedProfit)
                .actualAsset(actualAsset)
                .ruleFollowedAsset(actualAsset.add(missedProfit))
                .analysisStart(analysisStart)
                .analysisEnd(LocalDate.now(clock))
                .createdAt(LocalDateTime.now(clock))
                .ruleImpacts(ruleImpacts)
                .violationDetails(new ViolationDetails(violationDetails))
                .build();
    }

    public static RegretReport empty(Long roundId, Long exchangeId) {
        return RegretReport.builder()
                .roundId(roundId)
                .exchangeId(exchangeId)
                .totalViolations(0)
                .missedProfit(BigDecimal.ZERO)
                .actualAsset(BigDecimal.ZERO)
                .ruleFollowedAsset(BigDecimal.ZERO)
                .ruleImpacts(List.of())
                .violationDetails(new ViolationDetails(List.of()))
                .build();
    }

    private static BigDecimal sumLossAmounts(List<ViolationDetail> violationDetails) {
        BigDecimal sum =
                violationDetails.stream().map(ViolationDetail::getLossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.max(BigDecimal.ZERO);
    }

    public static RegretReport reconstitute(
            Long reportId,
            Long userId,
            Long roundId,
            Long exchangeId,
            int totalViolations,
            BigDecimal missedProfit,
            BigDecimal actualAsset,
            BigDecimal ruleFollowedAsset,
            LocalDate analysisStart,
            LocalDate analysisEnd,
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
                .actualAsset(actualAsset)
                .ruleFollowedAsset(ruleFollowedAsset)
                .analysisStart(analysisStart)
                .analysisEnd(analysisEnd)
                .createdAt(createdAt)
                .ruleImpacts(ruleImpacts)
                .violationDetails(new ViolationDetails(violationDetails))
                .build();
    }
}
