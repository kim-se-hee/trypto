package ksh.tryptobackend.investmentround.application.port.in.dto.command;

import java.math.BigDecimal;
import ksh.tryptobackend.common.domain.vo.RuleType;

public record StartRoundRuleCommand(RuleType ruleType, BigDecimal thresholdValue) {}
