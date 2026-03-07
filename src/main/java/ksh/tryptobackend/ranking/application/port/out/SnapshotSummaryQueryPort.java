package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.vo.SnapshotSummary;

import java.time.LocalDate;
import java.util.List;

public interface SnapshotSummaryQueryPort {

    List<SnapshotSummary> findLatestSummaries(LocalDate snapshotDate);
}
