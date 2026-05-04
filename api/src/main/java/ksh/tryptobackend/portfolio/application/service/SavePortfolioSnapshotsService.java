package ksh.tryptobackend.portfolio.application.service;

import java.util.List;
import ksh.tryptobackend.portfolio.application.port.in.SavePortfolioSnapshotsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotResult;
import ksh.tryptobackend.portfolio.application.port.out.PortfolioSnapshotCommandPort;
import ksh.tryptobackend.portfolio.domain.model.PortfolioSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SavePortfolioSnapshotsService implements SavePortfolioSnapshotsUseCase {

    private final PortfolioSnapshotCommandPort portfolioSnapshotCommandPort;

    @Override
    @Transactional
    public void saveAll(List<SnapshotResult> results) {
        List<PortfolioSnapshot> snapshots = results.stream().map(SnapshotResult::snapshot).toList();
        portfolioSnapshotCommandPort.saveAll(snapshots);
    }
}
