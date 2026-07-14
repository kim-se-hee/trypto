package ksh.tryptobackend.regretanalysis.adapter.out;

import java.util.Optional;
import ksh.tryptobackend.regretanalysis.adapter.out.persistence.entity.RegretReportJpaEntity;
import ksh.tryptobackend.regretanalysis.adapter.out.persistence.repository.RegretReportJpaRepository;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RegretReportQueryAdapter implements RegretReportQueryPort {

    private final RegretReportJpaRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<RegretReport> findByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return repository.findByRoundIdAndExchangeId(roundId, exchangeId).map(RegretReportJpaEntity::toDomain);
    }
}
