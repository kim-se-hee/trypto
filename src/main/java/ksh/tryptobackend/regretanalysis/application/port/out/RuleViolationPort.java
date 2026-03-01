package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.application.port.out.dto.RuleViolationRecord;

import java.util.List;

public interface RuleViolationPort {

    List<RuleViolationRecord> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId);
}
