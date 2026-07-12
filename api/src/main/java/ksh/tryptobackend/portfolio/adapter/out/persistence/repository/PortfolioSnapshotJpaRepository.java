package ksh.tryptobackend.portfolio.adapter.out.persistence.repository;

import ksh.tryptobackend.portfolio.adapter.out.persistence.entity.PortfolioSnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSnapshotJpaRepository extends JpaRepository<PortfolioSnapshotJpaEntity, Long> {}
