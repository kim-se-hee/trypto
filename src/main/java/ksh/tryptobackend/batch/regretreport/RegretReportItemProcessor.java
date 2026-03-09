package ksh.tryptobackend.batch.regretreport;

import ksh.tryptobackend.regretanalysis.application.port.in.GenerateRegretReportBatchUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GeneratedRegretReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class RegretReportItemProcessor implements ItemProcessor<RegretReportInput, GeneratedRegretReportResult> {

    private final GenerateRegretReportBatchUseCase generateRegretReportBatchUseCase;

    @Override
    public GeneratedRegretReportResult process(RegretReportInput input) {
        GenerateRegretReportCommand command = new GenerateRegretReportCommand(
            input.roundId(), input.userId(), input.exchangeId(),
            input.walletId(), input.startedAt());
        return generateRegretReportBatchUseCase.generateReport(command).orElse(null);
    }
}
