package ksh.tryptobackend.trading.application.support;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.dto.result.ViolationResult;
import ksh.tryptobackend.trading.application.port.out.RuleViolationQueryPort;
import ksh.tryptobackend.trading.application.port.out.WalletQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RuleViolationReader {

    private final RuleViolationQueryPort ruleViolationQueryPort;
    private final WalletQueryPort walletQueryPort;

    public List<ViolationResult> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId) {
        List<Long> walletIds = walletQueryPort.findWalletIdsByExchangeId(exchangeId);
        return ruleViolationQueryPort.findByRuleIdsAndWalletIds(ruleIds, walletIds).stream()
                .map(ViolationResult::from)
                .toList();
    }
}
