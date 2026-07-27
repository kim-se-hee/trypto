package ksh.tryptobackend.portfolio.application.service;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.DailyAssetTotalResult;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInfoResult;
import ksh.tryptobackend.portfolio.application.port.out.PortfolioSnapshotQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.DailyAssetTotal;
import ksh.tryptobackend.portfolio.domain.vo.SnapshotOverview;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindSnapshotsService implements FindSnapshotsUseCase {

    private final PortfolioSnapshotQueryPort portfolioSnapshotQueryPort;

    @Override
    public Optional<SnapshotInfoResult> findLatestByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return portfolioSnapshotQueryPort
                .findLatestByRoundIdAndExchangeId(roundId, exchangeId)
                .map(this::toResult);
    }

    @Override
    public List<SnapshotInfoResult> findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return portfolioSnapshotQueryPort.findAllByRoundIdAndExchangeId(roundId, exchangeId).stream()
                .map(this::toResult)
                .toList();
    }

    @Override
    public List<DailyAssetTotalResult> findDailyTotalsByRoundId(Long roundId) {
        return portfolioSnapshotQueryPort.findDailyTotalsByRoundId(roundId).stream()
                .map(this::toResult)
                .toList();
    }

    private DailyAssetTotalResult toResult(DailyAssetTotal total) {
        return new DailyAssetTotalResult(total.snapshotDate(), total.totalAssetKrw());
    }

    private SnapshotInfoResult toResult(SnapshotOverview info) {
        return new SnapshotInfoResult(
                info.snapshotId(), info.roundId(), info.exchangeId(), info.totalAsset(), info.snapshotDate());
    }
}
