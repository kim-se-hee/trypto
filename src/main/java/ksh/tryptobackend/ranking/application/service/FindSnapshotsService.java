package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.ranking.application.port.in.FindSnapshotsUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.result.SnapshotInfoResult;
import ksh.tryptobackend.ranking.application.port.out.PortfolioSnapshotQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.SnapshotInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FindSnapshotsService implements FindSnapshotsUseCase {

    private final PortfolioSnapshotQueryPort portfolioSnapshotQueryPort;

    @Override
    public Optional<SnapshotInfoResult> findLatestByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return portfolioSnapshotQueryPort.findLatestByRoundIdAndExchangeId(roundId, exchangeId)
            .map(this::toResult);
    }

    @Override
    public List<SnapshotInfoResult> findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return portfolioSnapshotQueryPort.findAllByRoundIdAndExchangeId(roundId, exchangeId).stream()
            .map(this::toResult)
            .toList();
    }

    private SnapshotInfoResult toResult(SnapshotInfo info) {
        return new SnapshotInfoResult(
            info.snapshotId(), info.roundId(), info.exchangeId(),
            info.totalAsset(), info.totalInvestment(),
            info.totalProfitRate(), info.snapshotDate()
        );
    }
}
