package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.regretanalysis.application.port.out.TradeViolationQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.TradeViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockTradeViolationQueryAdapter implements TradeViolationQueryPort {

    private final Map<String, List<TradeViolation>> violations = new ConcurrentHashMap<>();

    @Override
    public List<TradeViolation> findByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return violations.getOrDefault(key(roundId, exchangeId), List.of());
    }

    public void addViolation(Long roundId, Long exchangeId, TradeViolation violation) {
        violations.computeIfAbsent(key(roundId, exchangeId), k -> new ArrayList<>())
            .add(violation);
    }

    public void clear() {
        violations.clear();
    }

    private String key(Long roundId, Long exchangeId) {
        return roundId + ":" + exchangeId;
    }
}
