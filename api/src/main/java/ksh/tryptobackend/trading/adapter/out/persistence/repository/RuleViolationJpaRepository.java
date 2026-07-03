package ksh.tryptobackend.trading.adapter.out.persistence.repository;

import ksh.tryptobackend.trading.adapter.out.persistence.entity.RuleViolationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleViolationJpaRepository extends JpaRepository<RuleViolationJpaEntity, Long> {}
