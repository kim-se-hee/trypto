package ksh.tryptobackend.regretanalysis.application.port.in;

import ksh.tryptobackend.regretanalysis.application.port.in.dto.command.GenerateRegretReportCommand;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;

import java.util.Optional;

public interface GenerateRegretReportUseCase {

    Optional<RegretReport> generateReport(GenerateRegretReportCommand command);
}
