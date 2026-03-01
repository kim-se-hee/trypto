package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.application.port.out.dto.SnapshotInfo;

import java.util.List;
import java.util.Optional;

public interface SnapshotQueryPort {

    Optional<SnapshotInfo> findLatestByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<SnapshotInfo> findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
