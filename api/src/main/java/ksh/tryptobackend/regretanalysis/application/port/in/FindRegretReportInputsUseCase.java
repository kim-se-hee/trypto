package ksh.tryptobackend.regretanalysis.application.port.in;

import java.util.List;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportInputResult;

public interface FindRegretReportInputsUseCase {

    List<RegretReportInputResult> findAllInputs();
}
