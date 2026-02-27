package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import ksh.tryptobackend.trading.domain.vo.RuleType;

import java.math.BigDecimal;

public record StartRoundRuleResult(
    Long ruleId,
    RuleType ruleType,
    BigDecimal thresholdValue
) {
}
