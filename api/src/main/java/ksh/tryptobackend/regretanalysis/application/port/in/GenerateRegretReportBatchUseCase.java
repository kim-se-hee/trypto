package ksh.tryptobackend.regretanalysis.application.port.in;

import java.util.Optional;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GeneratedRegretReportResult;

public interface GenerateRegretReportBatchUseCase {

    Optional<GeneratedRegretReportResult> generateReport(GenerateRegretReportCommand command);
}
