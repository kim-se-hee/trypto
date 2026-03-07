package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.trading.application.port.out.ViolationRulePort;
import ksh.tryptobackend.trading.domain.model.ViolationRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockViolationRuleAdapter implements ViolationRulePort {

    private final Map<Long, List<ViolationRule>> rulesByWalletId = new ConcurrentHashMap<>();

    @Override
    public List<ViolationRule> findByWalletId(Long walletId) {
        return rulesByWalletId.getOrDefault(walletId, List.of());
    }

    public void addRule(Long walletId, ViolationRule rule) {
        rulesByWalletId.computeIfAbsent(walletId, k -> new ArrayList<>()).add(rule);
    }

    public void clear() {
        rulesByWalletId.clear();
    }
}
