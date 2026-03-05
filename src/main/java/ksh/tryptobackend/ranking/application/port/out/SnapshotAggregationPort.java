package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.application.port.out.dto.UserSnapshotSummary;

import java.time.LocalDate;
import java.util.List;

public interface SnapshotAggregationPort {

    List<UserSnapshotSummary> findLatestSummaries(LocalDate snapshotDate);
}
