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
    private final BigDecimal totalViolationLoss;
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
        BigDecimal totalViolationLoss = sumLossAmounts(violationDetails);

        return RegretReport.builder()
                .userId(userId)
                .roundId(roundId)
                .exchangeId(exchangeId)
                .totalViolations(violationDetails.size())
                .totalViolationLoss(totalViolationLoss)
                .actualAsset(actualAsset)
                .ruleFollowedAsset(actualAsset.add(totalViolationLoss))
                .analysisStart(analysisStart)
                .analysisEnd(LocalDate.now(clock))
                .createdAt(LocalDateTime.now(clock))
                .ruleImpacts(ruleImpacts)
                .violationDetails(new ViolationDetails(violationDetails))
                .build();
    }

    /** 위반 손실은 양수가 손해다. 원칙을 어긴 쪽이 오히려 이득이었으면 음수 그대로 남겨 화면이 그 사실을 말할 수 있게 한다. */
    private static BigDecimal sumLossAmounts(List<ViolationDetail> violationDetails) {
        return violationDetails.stream().map(ViolationDetail::getLossAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static RegretReport reconstitute(
            Long reportId,
            Long userId,
            Long roundId,
            Long exchangeId,
            int totalViolations,
            BigDecimal totalViolationLoss,
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
                .totalViolationLoss(totalViolationLoss)
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
