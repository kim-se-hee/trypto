package ksh.tryptobackend.portfolio.application.service;

import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotInfoResult;
import ksh.tryptobackend.portfolio.application.port.out.PortfolioSnapshotQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.dto.SnapshotInfo;
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
            .map(SnapshotInfo::toResult);
    }

    @Override
    public List<SnapshotInfoResult> findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return portfolioSnapshotQueryPort.findAllByRoundIdAndExchangeId(roundId, exchangeId).stream()
            .map(SnapshotInfo::toResult)
            .toList();
    }
}
