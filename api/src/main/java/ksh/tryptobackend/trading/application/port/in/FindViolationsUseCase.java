package ksh.tryptobackend.trading.application.port.in;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolationResult;

public interface FindViolationsUseCase {

    List<ViolationResult> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId);
}
