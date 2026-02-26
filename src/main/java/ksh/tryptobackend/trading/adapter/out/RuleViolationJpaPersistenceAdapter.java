package ksh.tryptobackend.trading.adapter.out;

import ksh.tryptobackend.trading.application.port.out.ViolationPersistencePort;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RuleViolationJpaPersistenceAdapter implements ViolationPersistencePort {

    private final RuleViolationJpaRepository repository;

    @Override
    public void saveAll(Long orderId, List<RuleViolation> violations) {
        List<RuleViolationJpaEntity> entities = violations.stream()
            .map(v -> RuleViolationJpaEntity.fromOrderViolation(orderId, v))
            .toList();
        repository.saveAll(entities);
    }
}
