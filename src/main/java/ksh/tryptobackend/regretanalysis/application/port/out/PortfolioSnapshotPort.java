package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.application.port.out.dto.AssetSnapshot;

import java.util.List;
import java.util.Optional;

public interface PortfolioSnapshotPort {

    Optional<AssetSnapshot> findLatestByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<AssetSnapshot> findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
