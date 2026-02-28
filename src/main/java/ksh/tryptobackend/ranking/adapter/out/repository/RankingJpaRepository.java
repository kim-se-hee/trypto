package ksh.tryptobackend.ranking.adapter.out.repository;

import ksh.tryptobackend.ranking.adapter.out.entity.RankingJpaEntity;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface RankingJpaRepository extends JpaRepository<RankingJpaEntity, Long> {

    @Query("SELECT MAX(r.referenceDate) FROM RankingJpaEntity r WHERE r.period = :period")
    Optional<LocalDate> findLatestReferenceDate(@Param("period") RankingPeriod period);
}
