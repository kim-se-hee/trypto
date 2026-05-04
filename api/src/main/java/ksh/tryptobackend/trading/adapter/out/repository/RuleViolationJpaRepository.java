package ksh.tryptobackend.trading.adapter.out.repository;

import ksh.tryptobackend.trading.adapter.out.entity.RuleViolationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleViolationJpaRepository extends JpaRepository<RuleViolationJpaEntity, Long> {}
