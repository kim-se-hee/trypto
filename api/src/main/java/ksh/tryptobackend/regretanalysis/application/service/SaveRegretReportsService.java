package ksh.tryptobackend.regretanalysis.application.service;

import java.util.List;
import ksh.tryptobackend.regretanalysis.application.port.in.SaveRegretReportsUseCase;
import ksh.tryptobackend.regretanalysis.application.port.out.RegretReportCommandPort;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SaveRegretReportsService implements SaveRegretReportsUseCase {

    private final RegretReportCommandPort regretReportCommandPort;

    @Override
    @Transactional
    public void saveAll(List<RegretReport> reports) {
        regretReportCommandPort.saveAll(reports);
    }
}
