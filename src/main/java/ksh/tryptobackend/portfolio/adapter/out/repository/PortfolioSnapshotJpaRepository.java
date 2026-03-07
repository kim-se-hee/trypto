package ksh.tryptobackend.portfolio.adapter.out.repository;

import ksh.tryptobackend.portfolio.adapter.out.entity.PortfolioSnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PortfolioSnapshotJpaRepository extends JpaRepository<PortfolioSnapshotJpaEntity, Long> {
}
