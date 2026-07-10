package ksh.tryptobackend.ranking.adapter.out.persistence.repository;

import ksh.tryptobackend.ranking.adapter.out.persistence.entity.RankingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingJpaRepository extends JpaRepository<RankingJpaEntity, Long> {}
