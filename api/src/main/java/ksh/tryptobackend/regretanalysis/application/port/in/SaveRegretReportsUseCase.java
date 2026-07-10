package ksh.tryptobackend.regretanalysis.application.port.in;

import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;

public interface SaveRegretReportsUseCase {

    void saveAll(List<RegretReport> reports);
}
