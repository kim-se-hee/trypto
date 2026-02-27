package ksh.tryptobackend.investmentround.application.port.out.dto;

import ksh.tryptobackend.common.domain.vo.RuleType;

import java.math.BigDecimal;

public record InvestmentRuleInfo(Long ruleId, RuleType ruleType, BigDecimal thresholdValue) {
}
