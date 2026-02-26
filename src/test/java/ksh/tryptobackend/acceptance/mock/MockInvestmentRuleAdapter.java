package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.trading.application.port.out.InvestmentRulePort;
import ksh.tryptobackend.trading.domain.model.InvestmentRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockInvestmentRuleAdapter implements InvestmentRulePort {

    private final Map<Long, List<InvestmentRule>> rulesByRoundId = new ConcurrentHashMap<>();

    @Override
    public List<InvestmentRule> findByRoundId(Long roundId) {
        return rulesByRoundId.getOrDefault(roundId, List.of());
    }

    public void addRule(Long roundId, InvestmentRule rule) {
        rulesByRoundId.computeIfAbsent(roundId, k -> new ArrayList<>()).add(rule);
    }

    public void clear() {
        rulesByRoundId.clear();
    }
}
