package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.model.RuleViolation;

import java.util.List;

public interface ViolationPersistencePort {

    void saveAll(Long orderId, List<RuleViolation> violations);
}
