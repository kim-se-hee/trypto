package ksh.tryptobackend.ranking.adapter.out.repository;

import ksh.tryptobackend.ranking.adapter.out.entity.PortfolioSnapshotJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PortfolioSnapshotJpaRepository extends JpaRepository<PortfolioSnapshotJpaEntity, Long> {

    Optional<PortfolioSnapshotJpaEntity> findTopByUserIdAndRoundIdOrderBySnapshotDateDesc(Long userId, Long roundId);
}
