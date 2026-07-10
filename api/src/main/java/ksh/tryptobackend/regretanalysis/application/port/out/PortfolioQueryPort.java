package ksh.tryptobackend.regretanalysis.application.port.out;

import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.vo.AssetTimeline;

public interface PortfolioQueryPort {

    AssetTimeline getAssetTimeline(Long roundId, Long exchangeId);

    AssetSnapshot getLatestSnapshot(Long roundId, Long exchangeId);
}
