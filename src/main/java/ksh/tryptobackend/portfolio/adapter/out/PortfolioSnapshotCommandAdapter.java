package ksh.tryptobackend.portfolio.adapter.out;

import ksh.tryptobackend.portfolio.adapter.out.entity.PortfolioSnapshotJpaEntity;
import ksh.tryptobackend.portfolio.adapter.out.repository.PortfolioSnapshotJpaRepository;
import ksh.tryptobackend.portfolio.application.port.out.PortfolioSnapshotCommandPort;
import ksh.tryptobackend.portfolio.domain.model.PortfolioSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PortfolioSnapshotCommandAdapter implements PortfolioSnapshotCommandPort {

    private final PortfolioSnapshotJpaRepository snapshotRepository;

    @Override
    public PortfolioSnapshot save(PortfolioSnapshot domain) {
        PortfolioSnapshotJpaEntity entity = PortfolioSnapshotJpaEntity.fromDomain(domain);
        PortfolioSnapshotJpaEntity saved = snapshotRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public List<PortfolioSnapshot> saveAll(List<PortfolioSnapshot> snapshots) {
        List<PortfolioSnapshotJpaEntity> entities = snapshots.stream()
            .map(PortfolioSnapshotJpaEntity::fromDomain)
            .toList();
        return snapshotRepository.saveAll(entities).stream()
            .map(PortfolioSnapshotJpaEntity::toDomain)
            .toList();
    }
}
