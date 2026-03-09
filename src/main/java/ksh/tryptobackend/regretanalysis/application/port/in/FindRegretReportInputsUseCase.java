package ksh.tryptobackend.regretanalysis.application.port.in;

import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportInputResult;

import java.util.List;

public interface FindRegretReportInputsUseCase {

    List<RegretReportInputResult> findAllInputs();
}
