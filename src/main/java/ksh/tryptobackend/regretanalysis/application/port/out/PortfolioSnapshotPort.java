package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;

import java.util.List;

public interface PortfolioSnapshotPort {

    AssetSnapshot getLatestByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<AssetSnapshot> findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
