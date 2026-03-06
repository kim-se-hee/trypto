package ksh.tryptobackend.regretanalysis.domain.vo;

import ksh.tryptobackend.common.domain.vo.RuleType;

import java.math.BigDecimal;

public record AnalysisRule(
    Long ruleId,
    RuleType ruleType,
    BigDecimal thresholdValue
) {
}
