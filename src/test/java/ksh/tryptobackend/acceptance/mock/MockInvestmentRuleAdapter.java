package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.trading.application.port.out.InvestmentRulePort;
import ksh.tryptobackend.trading.domain.model.ViolationRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockInvestmentRuleAdapter implements InvestmentRulePort {

    private final Map<Long, List<ViolationRule>> rulesByRoundId = new ConcurrentHashMap<>();

    @Override
    public List<ViolationRule> findByRoundId(Long roundId) {
        return rulesByRoundId.getOrDefault(roundId, List.of());
    }

    public void addRule(Long roundId, ViolationRule rule) {
        rulesByRoundId.computeIfAbsent(roundId, k -> new ArrayList<>()).add(rule);
    }

    public void clear() {
        rulesByRoundId.clear();
    }
}
