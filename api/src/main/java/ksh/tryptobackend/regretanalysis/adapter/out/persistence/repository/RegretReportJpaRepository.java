package ksh.tryptobackend.regretanalysis.adapter.out.persistence.repository;

import java.util.Optional;
import ksh.tryptobackend.regretanalysis.adapter.out.persistence.entity.RegretReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegretReportJpaRepository extends JpaRepository<RegretReportJpaEntity, Long> {

    Optional<RegretReportJpaEntity> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
