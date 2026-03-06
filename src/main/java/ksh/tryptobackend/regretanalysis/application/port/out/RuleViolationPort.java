package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.vo.RuleViolation;

import java.util.List;

public interface RuleViolationPort {

    List<RuleViolation> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId);
}
