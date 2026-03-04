package ksh.tryptobackend.batch.snapshot;

import ksh.tryptobackend.ranking.application.port.in.dto.result.SnapshotResult;
import ksh.tryptobackend.ranking.application.port.out.SnapshotPersistencePort;
import ksh.tryptobackend.ranking.domain.model.PortfolioSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class SnapshotItemWriter implements ItemWriter<SnapshotResult> {

    private final SnapshotPersistencePort snapshotPersistencePort;

    @Override
    public void write(Chunk<? extends SnapshotResult> chunk) {
        for (SnapshotResult output : chunk) {
            PortfolioSnapshot saved = snapshotPersistencePort.save(output.snapshot());
            snapshotPersistencePort.saveDetails(saved.getId(), output.details());
        }
    }
}
