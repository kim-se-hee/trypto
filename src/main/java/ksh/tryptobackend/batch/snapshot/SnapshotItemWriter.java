package ksh.tryptobackend.batch.snapshot;

import ksh.tryptobackend.ranking.application.port.in.dto.result.SnapshotResult;
import ksh.tryptobackend.ranking.application.port.out.SnapshotPersistencePort;
import ksh.tryptobackend.ranking.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.ranking.domain.model.SnapshotDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@StepScope
@RequiredArgsConstructor
public class SnapshotItemWriter implements ItemWriter<SnapshotResult> {

    private final SnapshotPersistencePort snapshotPersistencePort;

    @Override
    public void write(Chunk<? extends SnapshotResult> chunk) {
        List<SnapshotResult> items = List.copyOf(chunk.getItems());

        List<PortfolioSnapshot> snapshots = items.stream().map(SnapshotResult::snapshot).toList();
        List<PortfolioSnapshot> savedSnapshots = snapshotPersistencePort.saveAll(snapshots);

        Map<Long, List<SnapshotDetail>> detailsMap = new LinkedHashMap<>();
        for (int i = 0; i < savedSnapshots.size(); i++) {
            detailsMap.put(savedSnapshots.get(i).getId(), items.get(i).details());
        }
        snapshotPersistencePort.saveAllDetails(detailsMap);
    }
}
