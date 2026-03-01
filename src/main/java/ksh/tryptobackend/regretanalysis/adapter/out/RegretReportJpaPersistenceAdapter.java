package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.regretanalysis.adapter.out.entity.RegretReportJpaEntity;
import ksh.tryptobackend.regretanalysis.adapter.out.repository.RegretReportJpaRepository;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportPersistencePort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RegretReportJpaPersistenceAdapter implements RegretReportPersistencePort {

    private final RegretReportJpaRepository repository;

    @Override
    public Optional<RegretReport> findByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return repository.findByRoundIdAndExchangeId(roundId, exchangeId)
            .map(RegretReportJpaEntity::toDomain);
    }

    @Override
    public RegretReport getByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return repository.findByRoundIdAndExchangeId(roundId, exchangeId)
            .map(RegretReportJpaEntity::toDomain)
            .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
    }

    @Override
    public RegretReport save(RegretReport report) {
        RegretReportJpaEntity saved = repository.save(RegretReportJpaEntity.fromDomain(report));
        return saved.toDomain();
    }
}
