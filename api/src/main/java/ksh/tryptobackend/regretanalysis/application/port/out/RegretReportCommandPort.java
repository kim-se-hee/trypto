package ksh.tryptobackend.regretanalysis.application.port.out;

import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;

public interface RegretReportCommandPort {

    RegretReport save(RegretReport report);

    void saveAll(List<RegretReport> reports);
}
