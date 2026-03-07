package ksh.tryptobackend.batch.snapshot;

import ksh.tryptobackend.ranking.application.port.in.dto.result.SnapshotResult;
import ksh.tryptobackend.ranking.application.port.out.PortfolioSnapshotCommandPort;
import ksh.tryptobackend.ranking.domain.model.PortfolioSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
public class SnapshotItemWriter implements ItemWriter<SnapshotResult> {

    private final PortfolioSnapshotCommandPort portfolioSnapshotCommandPort;

    @Override
    public void write(Chunk<? extends SnapshotResult> chunk) {
        List<PortfolioSnapshot> snapshots = chunk.getItems().stream()
            .map(SnapshotResult::snapshot)
            .toList();
        portfolioSnapshotCommandPort.saveAll(snapshots);
    }
}
