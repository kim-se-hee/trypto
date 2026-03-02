package ksh.tryptobackend.regretanalysis.adapter.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.regretanalysis.adapter.out.entity.QRegretReportJpaEntity;
import ksh.tryptobackend.regretanalysis.adapter.out.entity.QViolationDetailJpaEntity;
import ksh.tryptobackend.regretanalysis.adapter.out.entity.RegretReportJpaEntity;
import ksh.tryptobackend.regretanalysis.adapter.out.entity.ViolationDetailJpaEntity;
import ksh.tryptobackend.regretanalysis.adapter.out.repository.RegretReportJpaRepository;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportPersistencePort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RegretReportJpaPersistenceAdapter implements RegretReportPersistencePort {

    private final RegretReportJpaRepository repository;
    private final JPAQueryFactory queryFactory;

    private static final QViolationDetailJpaEntity violationDetail = QViolationDetailJpaEntity.violationDetailJpaEntity;
    private static final QRegretReportJpaEntity report = QRegretReportJpaEntity.regretReportJpaEntity;

    @Override
    @Transactional(readOnly = true)
    public Optional<RegretReport> findByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return repository.findByRoundIdAndExchangeId(roundId, exchangeId)
            .map(RegretReportJpaEntity::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public RegretReport getByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return repository.findByRoundIdAndExchangeId(roundId, exchangeId)
            .map(RegretReportJpaEntity::toDomain)
            .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
    }

    @Override
    public boolean existsByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return repository.existsByRoundIdAndExchangeId(roundId, exchangeId);
    }

    @Override
    public List<ViolationDetail> findViolationDetailsByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        List<ViolationDetailJpaEntity> entities = queryFactory
            .selectFrom(violationDetail)
            .where(
                violationDetail.report.roundId.eq(roundId),
                violationDetail.report.exchangeId.eq(exchangeId)
            )
            .fetch();

        return entities.stream()
            .map(ViolationDetailJpaEntity::toDomain)
            .toList();
    }

    @Override
    public RegretReport save(RegretReport report) {
        RegretReportJpaEntity saved = repository.save(RegretReportJpaEntity.fromDomain(report));
        return saved.toDomain();
    }
}
