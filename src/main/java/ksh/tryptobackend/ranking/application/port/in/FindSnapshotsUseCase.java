package ksh.tryptobackend.ranking.application.port.in;

import ksh.tryptobackend.ranking.application.port.in.dto.result.SnapshotInfoResult;

import java.util.List;
import java.util.Optional;

public interface FindSnapshotsUseCase {

    Optional<SnapshotInfoResult> findLatestByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<SnapshotInfoResult> findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
