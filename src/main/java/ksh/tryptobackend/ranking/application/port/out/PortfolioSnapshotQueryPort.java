package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.application.port.out.dto.SnapshotDetailProjection;
import ksh.tryptobackend.ranking.application.port.out.dto.SnapshotInfo;
import ksh.tryptobackend.ranking.application.port.out.dto.UserSnapshotSummary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotQueryPort {

    List<SnapshotDetailProjection> findLatestSnapshotDetails(Long userId, Long roundId);

    Optional<SnapshotInfo> findLatestByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<SnapshotInfo> findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<UserSnapshotSummary> findLatestSummaries(LocalDate snapshotDate);
}
