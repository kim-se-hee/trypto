package ksh.tryptobackend.portfolio.application.service;

import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotSummariesUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotSummaryResult;
import ksh.tryptobackend.portfolio.application.port.out.PortfolioSnapshotQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.UserSnapshotSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FindSnapshotSummariesService implements FindSnapshotSummariesUseCase {

    private final PortfolioSnapshotQueryPort portfolioSnapshotQueryPort;

    @Override
    public List<SnapshotSummaryResult> findLatestSummaries(LocalDate snapshotDate) {
        return portfolioSnapshotQueryPort.findLatestSummaries(snapshotDate).stream()
            .map(this::toResult)
            .toList();
    }

    private SnapshotSummaryResult toResult(UserSnapshotSummary summary) {
        return new SnapshotSummaryResult(summary.userId(), summary.roundId(),
            summary.totalAssetKrw(), summary.totalInvestmentKrw());
    }
}
