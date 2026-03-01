package ksh.tryptobackend.regretanalysis.application.port.in;

import ksh.tryptobackend.regretanalysis.application.port.in.dto.query.GetRegretReportQuery;
import ksh.tryptobackend.regretanalysis.application.port.in.dto.result.GetRegretReportResult;

public interface GetRegretReportUseCase {

    GetRegretReportResult getRegretReport(GetRegretReportQuery query);
}
