package ksh.tryptobackend.investmentround.application.port.in.dto.command;

import ksh.tryptobackend.trading.domain.vo.RuleType;

import java.math.BigDecimal;

public record StartRoundRuleCommand(
    RuleType ruleType,
    BigDecimal thresholdValue
) {
}
