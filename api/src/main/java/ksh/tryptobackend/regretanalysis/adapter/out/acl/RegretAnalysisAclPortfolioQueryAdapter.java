package ksh.tryptobackend.regretanalysis.adapter.out.acl;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.DailyAssetTotalResult;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInfoResult;
import ksh.tryptobackend.regretanalysis.application.port.out.PortfolioQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.vo.AssetTimeline;
import ksh.tryptobackend.regretanalysis.domain.vo.DailyAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RegretAnalysisAclPortfolioQueryAdapter implements PortfolioQueryPort {

    private final FindSnapshotsUseCase findSnapshotsUseCase;

    @Override
    public AssetTimeline getRoundAssetTimeline(Long roundId) {
        List<DailyAsset> dailyAssets = findSnapshotsUseCase.findDailyTotalsByRoundId(roundId).stream()
                .map(this::toDailyAsset)
                .toList();
        return AssetTimeline.of(dailyAssets);
    }

    @Override
    public Optional<AssetSnapshot> findLatestSnapshot(Long roundId, Long exchangeId) {
        return findSnapshotsUseCase
                .findLatestByRoundIdAndExchangeId(roundId, exchangeId)
                .map(this::toAssetSnapshot);
    }

    private DailyAsset toDailyAsset(DailyAssetTotalResult result) {
        return new DailyAsset(result.snapshotDate(), result.totalAssetKrw());
    }

    private AssetSnapshot toAssetSnapshot(SnapshotInfoResult result) {
        return AssetSnapshot.reconstitute(
                result.snapshotId(), result.roundId(), result.exchangeId(), result.totalAsset(), result.snapshotDate());
    }
}
