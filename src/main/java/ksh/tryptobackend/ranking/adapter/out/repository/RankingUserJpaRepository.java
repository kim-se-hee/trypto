package ksh.tryptobackend.ranking.adapter.out.repository;

import ksh.tryptobackend.ranking.adapter.out.entity.RankingUserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RankingUserJpaRepository extends JpaRepository<RankingUserJpaEntity, Long> {
}
