package ksh.tryptobackend.ranking.adapter.out.acl;

import java.util.List;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotDetailsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotDetailResult;
import ksh.tryptobackend.ranking.application.port.out.PortfolioQueryPort;
import ksh.tryptobackend.ranking.domain.vo.Holding;
import ksh.tryptobackend.ranking.domain.vo.Holdings;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AclPortfolioQueryAdapter implements PortfolioQueryPort {

    private final FindSnapshotDetailsUseCase findSnapshotDetailsUseCase;

    @Override
    public Holdings findLatestHoldings(Long userId, Long roundId) {
        List<Holding> holdings =
                findSnapshotDetailsUseCase.findLatestSnapshotDetails(userId, roundId).stream()
                        .map(this::toHolding)
                        .toList();
        return new Holdings(holdings);
    }

    private Holding toHolding(SnapshotDetailResult detail) {
        return new Holding(
                detail.coinId(), detail.exchangeId(), detail.assetRatio(), detail.profitRate());
    }
}
