package ksh.tryptobackend.regretanalysis.application.port.in;

import java.util.Optional;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;

public interface GenerateRegretReportUseCase {

    Optional<RegretReport> generateReport(GenerateRegretReportCommand command);
}
