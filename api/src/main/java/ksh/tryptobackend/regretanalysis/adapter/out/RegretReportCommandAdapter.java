package ksh.tryptobackend.regretanalysis.adapter.out;

import java.util.List;
import ksh.tryptobackend.regretanalysis.adapter.out.persistence.entity.RegretReportJpaEntity;
import ksh.tryptobackend.regretanalysis.adapter.out.persistence.repository.RegretReportJpaRepository;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportCommandPort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegretReportCommandAdapter implements RegretReportCommandPort {

    private final RegretReportJpaRepository repository;

    @Override
    public RegretReport save(RegretReport domain) {
        RegretReportJpaEntity saved = repository.save(RegretReportJpaEntity.fromDomain(domain));
        return saved.toDomain();
    }

    @Override
    public void saveAll(List<RegretReport> reports) {
        List<RegretReportJpaEntity> entities =
                reports.stream().map(RegretReportJpaEntity::fromDomain).toList();
        repository.saveAll(entities);
    }
}
