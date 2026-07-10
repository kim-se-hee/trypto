package ksh.tryptobackend.regretanalysis.adapter.out.acl;

import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInfoResult;
import ksh.tryptobackend.regretanalysis.application.port.out.PortfolioQueryPort;
import ksh.tryptobackend.regretanalysis.domain.model.AssetSnapshot;
import ksh.tryptobackend.regretanalysis.domain.vo.AssetTimeline;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("regretanalysisAclPortfolioQueryAdapter")
@RequiredArgsConstructor
public class AclPortfolioQueryAdapter implements PortfolioQueryPort {

    private final FindSnapshotsUseCase findSnapshotsUseCase;

    @Override
    public AssetTimeline getAssetTimeline(Long roundId, Long exchangeId) {
        List<AssetSnapshot> snapshots =
                findSnapshotsUseCase.findAllByRoundIdAndExchangeId(roundId, exchangeId).stream()
                        .map(this::toAssetSnapshot)
                        .toList();
        return AssetTimeline.of(snapshots);
    }

    @Override
    public AssetSnapshot getLatestSnapshot(Long roundId, Long exchangeId) {
        return findSnapshotsUseCase
                .findLatestByRoundIdAndExchangeId(roundId, exchangeId)
                .map(this::toAssetSnapshot)
                .orElseThrow(() -> new CustomException(ErrorCode.SNAPSHOT_NOT_FOUND));
    }

    private AssetSnapshot toAssetSnapshot(SnapshotInfoResult result) {
        return AssetSnapshot.reconstitute(
                result.snapshotId(),
                result.roundId(),
                result.exchangeId(),
                result.totalAsset(),
                result.totalInvestment(),
                result.totalProfitRate(),
                result.snapshotDate());
    }
}
