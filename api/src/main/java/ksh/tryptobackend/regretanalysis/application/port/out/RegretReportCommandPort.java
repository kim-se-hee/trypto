package ksh.tryptobackend.regretanalysis.application.port.out;

import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;

public interface RegretReportCommandPort {

    void saveAll(List<RegretReport> reports);
}
