package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.application.port.in.dto.result.ViolationResult;

import java.util.List;

public interface FindViolationsUseCase {

    List<ViolationResult> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId);
}
