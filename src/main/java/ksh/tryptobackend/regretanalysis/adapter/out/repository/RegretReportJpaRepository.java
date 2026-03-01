package ksh.tryptobackend.regretanalysis.adapter.out.repository;

import ksh.tryptobackend.regretanalysis.adapter.out.entity.RegretReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegretReportJpaRepository extends JpaRepository<RegretReportJpaEntity, Long> {

    Optional<RegretReportJpaEntity> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
