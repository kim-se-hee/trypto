package ksh.tryptobackend.regretanalysis.domain.model;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RuleImpact {

    private final Long ruleImpactId;
    private final Long reportId;
    private final Long ruleId;
    private final int violationCount;
    private final BigDecimal totalLossAmount;

    public static RuleImpact create(Long ruleId, int violationCount, BigDecimal totalLossAmount) {
        return RuleImpact.builder()
                .ruleId(ruleId)
                .violationCount(violationCount)
                .totalLossAmount(totalLossAmount)
                .build();
    }

    public static RuleImpact reconstitute(
            Long ruleImpactId, Long reportId, Long ruleId, int violationCount, BigDecimal totalLossAmount) {
        return RuleImpact.builder()
                .ruleImpactId(ruleImpactId)
                .reportId(reportId)
                .ruleId(ruleId)
                .violationCount(violationCount)
                .totalLossAmount(totalLossAmount)
                .build();
    }
}
