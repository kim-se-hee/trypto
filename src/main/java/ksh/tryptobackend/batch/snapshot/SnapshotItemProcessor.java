package ksh.tryptobackend.batch.snapshot;

import ksh.tryptobackend.ranking.application.port.in.TakePortfolioSnapshotUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.command.TakeSnapshotCommand;
import ksh.tryptobackend.ranking.application.port.in.dto.result.SnapshotResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@StepScope
@RequiredArgsConstructor
public class SnapshotItemProcessor implements ItemProcessor<SnapshotInput, SnapshotResult> {

    private final TakePortfolioSnapshotUseCase takePortfolioSnapshotUseCase;

    @Value("#{jobParameters['snapshotDate']}")
    private String snapshotDateParam;

    private LocalDate snapshotDate;

    @Override
    public SnapshotResult process(SnapshotInput input) {
        TakeSnapshotCommand command = new TakeSnapshotCommand(
            input.roundId(), input.userId(), input.exchangeId(),
            input.walletId(), input.seedAmount(), getSnapshotDate());
        return takePortfolioSnapshotUseCase.takeSnapshot(command);
    }

    private LocalDate getSnapshotDate() {
        if (snapshotDate == null) {
            snapshotDate = LocalDate.parse(snapshotDateParam);
        }
        return snapshotDate;
    }
}
