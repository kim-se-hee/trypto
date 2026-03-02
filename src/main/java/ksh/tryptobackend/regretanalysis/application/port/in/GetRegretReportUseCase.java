package ksh.tryptobackend.regretanalysis.application.port.in;

import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.RegretReportResult;

public interface GetRegretReportUseCase {

    RegretReportResult getRegretReport(GetRegretReportQuery query);
}
