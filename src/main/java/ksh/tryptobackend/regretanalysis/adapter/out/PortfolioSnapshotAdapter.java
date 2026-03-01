package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.ranking.application.port.out.SnapshotQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.SnapshotInfo;
import ksh.tryptobackend.regretanalysis.application.port.out.PortfolioSnapshotPort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.AssetSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PortfolioSnapshotAdapter implements PortfolioSnapshotPort {

    private final SnapshotQueryPort snapshotQueryPort;

    @Override
    public Optional<AssetSnapshot> findLatestByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return snapshotQueryPort.findLatestByRoundIdAndExchangeId(roundId, exchangeId)
            .map(this::toAssetSnapshot);
    }

    @Override
    public List<AssetSnapshot> findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return snapshotQueryPort.findAllByRoundIdAndExchangeId(roundId, exchangeId).stream()
            .map(this::toAssetSnapshot)
            .toList();
    }

    private AssetSnapshot toAssetSnapshot(SnapshotInfo info) {
        return new AssetSnapshot(
            info.snapshotId(), info.roundId(), info.exchangeId(),
            info.totalAsset(), info.totalInvestment(),
            info.totalProfitRate(), info.snapshotDate()
        );
    }
}
