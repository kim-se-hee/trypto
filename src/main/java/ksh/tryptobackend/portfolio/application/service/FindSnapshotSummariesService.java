package ksh.tryptobackend.portfolio.application.service;

import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotSummariesUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotSummaryResult;
import ksh.tryptobackend.portfolio.application.port.out.PortfolioSnapshotQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.dto.UserSnapshotSummary;
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
            .map(UserSnapshotSummary::toResult)
            .toList();
    }
}
