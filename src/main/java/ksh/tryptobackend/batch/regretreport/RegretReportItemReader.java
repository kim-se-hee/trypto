package ksh.tryptobackend.batch.regretreport;

import ksh.tryptobackend.regretanalysis.application.port.in.FindRegretReportInputsUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportInputResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

@Component
@StepScope
@RequiredArgsConstructor
public class RegretReportItemReader implements ItemReader<RegretReportInput> {

    private final FindRegretReportInputsUseCase findRegretReportInputsUseCase;

    private Iterator<RegretReportInput> iterator;

    @Override
    public RegretReportInput read() {
        if (iterator == null) {
            iterator = buildInputList().iterator();
        }
        return iterator.hasNext() ? iterator.next() : null;
    }

    private List<RegretReportInput> buildInputList() {
        return findRegretReportInputsUseCase.findAllInputs().stream()
            .map(this::toInput)
            .toList();
    }

    private RegretReportInput toInput(RegretReportInputResult result) {
        return new RegretReportInput(
            result.roundId(), result.userId(), result.exchangeId(),
            result.walletId(), result.startedAt());
    }
}
