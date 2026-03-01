package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.application.port.out.dto.ViolationInfo;

import java.util.List;

public interface RuleViolationPort {

    List<ViolationInfo> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId);
}
