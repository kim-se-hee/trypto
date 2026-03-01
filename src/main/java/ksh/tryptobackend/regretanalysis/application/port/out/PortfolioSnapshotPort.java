package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.application.port.out.dto.SnapshotInfo;

import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotPort {

    Optional<SnapshotInfo> findLatestByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<SnapshotInfo> findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
