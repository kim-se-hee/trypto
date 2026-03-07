package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.application.port.out.dto.ViolationInfo;

import java.util.List;

public interface RuleViolationQueryPort {

    List<ViolationInfo> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId);
}
