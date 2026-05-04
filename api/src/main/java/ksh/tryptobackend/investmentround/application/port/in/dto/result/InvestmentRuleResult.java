package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.math.BigDecimal;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.investmentround.domain.model.RuleSetting;

public record InvestmentRuleResult(Long ruleId, RuleType ruleType, BigDecimal thresholdValue) {

    public static InvestmentRuleResult from(RuleSetting rule) {
        return new InvestmentRuleResult(
                rule.getRuleId(), rule.getRuleType(), rule.getThresholdValue());
    }
}
