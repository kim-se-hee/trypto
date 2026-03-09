package ksh.tryptobackend.ranking.adapter.out;

import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotSummariesUseCase;
import ksh.tryptobackend.ranking.application.port.out.SnapshotSummaryQueryPort;
import ksh.tryptobackend.ranking.domain.vo.SnapshotSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class SnapshotSummaryQueryAdapter implements SnapshotSummaryQueryPort {

    private final FindSnapshotSummariesUseCase findSnapshotSummariesUseCase;

    @Override
    public List<SnapshotSummary> findLatestSummaries(LocalDate snapshotDate) {
        return findSnapshotSummariesUseCase.findLatestSummaries(snapshotDate).stream()
            .map(r -> new SnapshotSummary(r.userId(), r.roundId(), r.totalAssetKrw(), r.totalInvestmentKrw()))
            .toList();
    }
}
