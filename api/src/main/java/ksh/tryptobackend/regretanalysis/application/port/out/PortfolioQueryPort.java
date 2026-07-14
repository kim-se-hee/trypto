package ksh.tryptobackend.regretanalysis.application.port.out;

import java.util.Optional;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.vo.AssetTimeline;

public interface PortfolioQueryPort {

    AssetTimeline getAssetTimeline(Long roundId, Long exchangeId);

    Optional<AssetSnapshot> findLatestSnapshot(Long roundId, Long exchangeId);
}
