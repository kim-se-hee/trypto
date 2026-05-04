package ksh.tryptobackend.trading.application.port.out;

import java.util.List;
import ksh.tryptobackend.trading.domain.vo.RuleViolationRef;

public interface RuleViolationQueryPort {

    List<RuleViolationRef> findByRuleIdsAndWalletIds(List<Long> ruleIds, List<Long> walletIds);
}
