package ksh.tryptobackend.portfolio.application.port.in;

import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotSummaryResult;

public interface FindSnapshotSummariesUseCase {

    List<SnapshotSummaryResult> findLatestSummaries(LocalDate snapshotDate);
}
