package ksh.tryptobackend.trading.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleViolationJpaRepository extends JpaRepository<RuleViolationJpaEntity, Long> {
}
