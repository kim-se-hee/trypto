package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.math.BigDecimal;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.investmentround.domain.model.RuleSetting;

public record StartRoundRuleResult(Long ruleId, RuleType ruleType, BigDecimal thresholdValue) {

    public static StartRoundRuleResult from(RuleSetting rule) {
        return new StartRoundRuleResult(
                rule.getRuleId(), rule.getRuleType(), rule.getThresholdValue());
    }
}
