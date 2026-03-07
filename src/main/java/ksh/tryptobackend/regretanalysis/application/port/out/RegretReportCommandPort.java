package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;

import java.util.List;

public interface RegretReportCommandPort {

    RegretReport save(RegretReport report);

    void saveAll(List<RegretReport> reports);
}
