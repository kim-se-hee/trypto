package ksh.tryptobackend.ranking.adapter.out.acl;

import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotDetailsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotSummariesUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotDetailResult;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotSummaryResult;
import ksh.tryptobackend.ranking.application.port.out.PortfolioQueryPort;
import ksh.tryptobackend.ranking.domain.vo.Holding;
import ksh.tryptobackend.ranking.domain.vo.Holdings;
import ksh.tryptobackend.ranking.domain.vo.SnapshotSummaries;
import ksh.tryptobackend.ranking.domain.vo.SnapshotSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingAclPortfolioQueryAdapter implements PortfolioQueryPort {

    private final FindSnapshotDetailsUseCase findSnapshotDetailsUseCase;
    private final FindSnapshotSummariesUseCase findSnapshotSummariesUseCase;

    @Override
    public Holdings findLatestHoldings(Long userId, Long roundId) {
        List<Holding> holdings =
                findSnapshotDetailsUseCase.findLatestSnapshotDetails(userId, roundId).stream()
                        .map(this::toHolding)
                        .toList();
        return new Holdings(holdings);
    }

    @Override
    public SnapshotSummaries findLatestSummaries(LocalDate snapshotDate) {
        List<SnapshotSummary> summaries =
                findSnapshotSummariesUseCase.findLatestSummaries(snapshotDate).stream()
                        .map(this::toSnapshotSummary)
                        .toList();
        return new SnapshotSummaries(summaries);
    }

    private Holding toHolding(SnapshotDetailResult detail) {
        return new Holding(
                detail.coinId(), detail.exchangeId(), detail.assetRatio(), detail.profitRate());
    }

    private SnapshotSummary toSnapshotSummary(SnapshotSummaryResult result) {
        return new SnapshotSummary(
                result.userId(),
                result.roundId(),
                result.totalAssetKrw(),
                result.totalInvestmentKrw());
    }
}
