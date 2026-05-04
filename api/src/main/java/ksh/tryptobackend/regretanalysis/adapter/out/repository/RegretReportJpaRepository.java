package ksh.tryptobackend.regretanalysis.adapter.out.repository;

import java.util.Optional;
import ksh.tryptobackend.regretanalysis.adapter.out.entity.RegretReportJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegretReportJpaRepository extends JpaRepository<RegretReportJpaEntity, Long> {

    Optional<RegretReportJpaEntity> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    boolean existsByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
