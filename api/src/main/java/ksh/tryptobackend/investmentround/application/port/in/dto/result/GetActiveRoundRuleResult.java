package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.math.BigDecimal;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.investmentround.domain.model.Rule;

public record GetActiveRoundRuleResult(Long ruleId, RuleType ruleType, BigDecimal thresholdValue) {

    public static GetActiveRoundRuleResult from(Rule rule) {
        return new GetActiveRoundRuleResult(rule.id(), rule.ruleType(), rule.thresholdValue());
    }
}
