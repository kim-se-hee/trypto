package ksh.tryptobackend.batch.snapshot;

import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotInputsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInputResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
public class SnapshotItemReader implements ItemReader<SnapshotInput> {

    private final FindSnapshotInputsUseCase findSnapshotInputsUseCase;

    private Iterator<SnapshotInput> iterator;

    @Override
    public SnapshotInput read() {
        if (iterator == null) {
            iterator = buildInputList().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<SnapshotInput> buildInputList() {
        return findSnapshotInputsUseCase.findAllSnapshotInputs().stream()
            .map(this::toSnapshotInput)
            .toList();
    }

    private SnapshotInput toSnapshotInput(SnapshotInputResult result) {
        return new SnapshotInput(
            result.roundId(), result.userId(), result.exchangeId(),
            result.walletId(), result.seedAmount());
    }
}
