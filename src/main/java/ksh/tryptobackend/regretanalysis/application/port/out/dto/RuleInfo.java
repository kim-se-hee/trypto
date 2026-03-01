package ksh.tryptobackend.regretanalysis.application.port.out.dto;

import ksh.tryptobackend.common.domain.vo.RuleType;

import java.math.BigDecimal;

public record RuleInfo(
    Long ruleId,
    RuleType ruleType,
    BigDecimal thresholdValue
) {
}
