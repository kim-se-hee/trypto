package ksh.tryptobackend.batch.snapshot;

import ksh.tryptobackend.portfolio.application.port.in.SavePortfolioSnapshotsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotResult;
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

    private final SavePortfolioSnapshotsUseCase savePortfolioSnapshotsUseCase;

    @Override
    public void write(Chunk<? extends SnapshotResult> chunk) {
        List<SnapshotResult> results = chunk.getItems().stream()
            .map(item -> (SnapshotResult) item)
            .toList();
        savePortfolioSnapshotsUseCase.saveAll(results);
    }
}
