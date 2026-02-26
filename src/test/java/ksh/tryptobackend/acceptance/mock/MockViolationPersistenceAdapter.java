package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.trading.application.port.out.ViolationPersistencePort;
import ksh.tryptobackend.trading.domain.model.RuleViolation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockViolationPersistenceAdapter implements ViolationPersistencePort {

    private final Map<Long, List<RuleViolation>> violationsByOrderId = new ConcurrentHashMap<>();

    @Override
    public void saveAll(Long orderId, List<RuleViolation> violations) {
        violationsByOrderId.computeIfAbsent(orderId, k -> new ArrayList<>()).addAll(violations);
    }

    public List<RuleViolation> getViolations(Long orderId) {
        return violationsByOrderId.getOrDefault(orderId, List.of());
    }

    public void clear() {
        violationsByOrderId.clear();
    }
}
