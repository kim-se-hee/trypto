package ksh.tryptobackend.regretanalysis.application.port.in;

import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GeneratedRegretReportResult;

import java.util.Optional;

public interface GenerateRegretReportBatchUseCase {

    Optional<GeneratedRegretReportResult> generateReport(GenerateRegretReportCommand command);
}
