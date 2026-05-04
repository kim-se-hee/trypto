package ksh.tryptobackend.regretanalysis.application.port.in;

import java.util.List;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GeneratedRegretReportResult;

public interface SaveRegretReportsUseCase {

    void saveAll(List<GeneratedRegretReportResult> results);
}
