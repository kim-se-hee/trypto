package ksh.tryptobackend.batch.regretreport;

import ksh.tryptobackend.regretanalysis.application.port.in.GenerateRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
@StepScope
@RequiredArgsConstructor
public class RegretReportItemProcessor implements ItemProcessor<RegretReportInput, RegretReport> {

    private final GenerateRegretReportUseCase generateRegretReportUseCase;

    @Override
    public RegretReport process(RegretReportInput input) {
        GenerateRegretReportCommand command = new GenerateRegretReportCommand(
            input.roundId(), input.userId(), input.exchangeId(),
            input.walletId(), input.startedAt());
        return generateRegretReportUseCase.generateReport(command).orElse(null);
    }
}
