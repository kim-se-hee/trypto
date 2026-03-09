package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.regretanalysis.application.port.in.GenerateRegretReportBatchUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.GenerateRegretReportUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GeneratedRegretReportResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GenerateRegretReportBatchService implements GenerateRegretReportBatchUseCase {

    private final GenerateRegretReportUseCase generateRegretReportUseCase;

    @Override
    public Optional<GeneratedRegretReportResult> generateReport(GenerateRegretReportCommand command) {
        return generateRegretReportUseCase.generateReport(command)
            .map(GeneratedRegretReportResult::new);
    }
}
