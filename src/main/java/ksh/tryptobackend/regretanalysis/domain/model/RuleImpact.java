package ksh.tryptobackend.regretanalysis.domain.model;

import ksh.tryptobackend.regretanalysis.domain.vo.ImpactGap;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RuleImpact {

    private final Long ruleImpactId;
    private final Long reportId;
    private final Long ruleId;
    private final int violationCount;
    private final BigDecimal totalLossAmount;
    private final ImpactGap impactGap;

    public static RuleImpact create(Long ruleId, int violationCount,
                                    BigDecimal totalLossAmount, ImpactGap impactGap) {
        return RuleImpact.builder()
            .ruleId(ruleId)
            .violationCount(violationCount)
            .totalLossAmount(totalLossAmount)
            .impactGap(impactGap)
            .build();
    }

    public static RuleImpact reconstitute(Long ruleImpactId, Long reportId, Long ruleId,
                                          int violationCount, BigDecimal totalLossAmount,
                                          ImpactGap impactGap) {
        return RuleImpact.builder()
            .ruleImpactId(ruleImpactId)
            .reportId(reportId)
            .ruleId(ruleId)
            .violationCount(violationCount)
            .totalLossAmount(totalLossAmount)
            .impactGap(impactGap)
            .build();
    }
}
