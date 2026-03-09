package ksh.tryptobackend.regretanalysis.application.port.in;

import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GeneratedRegretReportResult;

import java.util.List;

public interface SaveRegretReportsUseCase {

    void saveAll(List<GeneratedRegretReportResult> results);
}
