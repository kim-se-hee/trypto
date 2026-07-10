package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.math.BigDecimal;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.investmentround.domain.model.Rule;

public record InvestmentRuleResult(Long ruleId, RuleType ruleType, BigDecimal thresholdValue) {

    public static InvestmentRuleResult from(Rule rule) {
        return new InvestmentRuleResult(rule.id(), rule.ruleType(), rule.thresholdValue());
    }
}
