package ksh.tryptobackend.ranking.adapter.out.repository;

import ksh.tryptobackend.ranking.adapter.out.entity.RankingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingJpaRepository extends JpaRepository<RankingJpaEntity, Long> {
}
