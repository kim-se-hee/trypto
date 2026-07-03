package ksh.tryptobackend.trading.adapter.out.persistence;

import java.util.List;
import ksh.tryptobackend.trading.adapter.out.persistence.entity.RuleViolationJpaEntity;
import ksh.tryptobackend.trading.adapter.out.persistence.repository.RuleViolationJpaRepository;
import ksh.tryptobackend.trading.application.port.out.RuleViolationCommandPort;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaRuleViolationCommandAdapter implements RuleViolationCommandPort {

    private final RuleViolationJpaRepository ruleViolationJpaRepository;

    @Override
    public void appendAll(Long orderId, List<RuleViolation> violations) {
        if (violations.isEmpty()) {
            return;
        }
        List<RuleViolationJpaEntity> entities =
                violations.stream()
                        .map(v -> RuleViolationJpaEntity.fromOrderViolation(orderId, v))
                        .toList();
        ruleViolationJpaRepository.saveAll(entities);
    }
}
