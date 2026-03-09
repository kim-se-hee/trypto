package ksh.tryptobackend.regretanalysis.application.service;

import ksh.tryptobackend.regretanalysis.application.port.in.SaveRegretReportsUseCase;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GeneratedRegretReportResult;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportCommandPort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SaveRegretReportsService implements SaveRegretReportsUseCase {

    private final RegretReportCommandPort regretReportCommandPort;

    @Override
    @Transactional
    public void saveAll(List<GeneratedRegretReportResult> results) {
        List<RegretReport> reports = results.stream()
            .map(GeneratedRegretReportResult::report)
            .toList();
        regretReportCommandPort.saveAll(reports);
    }
}
