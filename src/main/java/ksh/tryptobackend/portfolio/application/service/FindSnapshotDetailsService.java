package ksh.tryptobackend.portfolio.application.service;

import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotDetailsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotDetailResult;
import ksh.tryptobackend.portfolio.application.port.out.PortfolioSnapshotQueryPort;
import ksh.tryptobackend.portfolio.domain.vo.HoldingSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindSnapshotDetailsService implements FindSnapshotDetailsUseCase {

    private final PortfolioSnapshotQueryPort portfolioSnapshotQueryPort;

    @Override
    public List<SnapshotDetailResult> findLatestSnapshotDetails(Long userId, Long roundId) {
        return portfolioSnapshotQueryPort.findLatestSnapshotDetails(userId, roundId).stream()
            .map(this::toResult)
            .toList();
    }

    private SnapshotDetailResult toResult(HoldingSummary detail) {
        return new SnapshotDetailResult(detail.coinId(), detail.exchangeId(),
            detail.assetRatio(), detail.profitRate());
    }
}
