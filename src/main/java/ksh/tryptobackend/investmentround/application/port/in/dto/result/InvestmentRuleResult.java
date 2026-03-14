package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.investmentround.domain.model.RuleSetting;

import java.math.BigDecimal;

public record InvestmentRuleResult(Long ruleId, RuleType ruleType, BigDecimal thresholdValue) {

    public static InvestmentRuleResult from(RuleSetting rule) {
        return new InvestmentRuleResult(rule.getRuleId(), rule.getRuleType(), rule.getThresholdValue());
    }
}
