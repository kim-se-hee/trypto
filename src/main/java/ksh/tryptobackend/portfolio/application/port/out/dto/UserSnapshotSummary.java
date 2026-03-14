package ksh.tryptobackend.portfolio.application.port.out.dto;

import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotSummaryResult;

import java.math.BigDecimal;

public record UserSnapshotSummary(
    Long userId,
    Long roundId,
    BigDecimal totalAssetKrw,
    BigDecimal totalInvestmentKrw
) {

    public SnapshotSummaryResult toResult() {
        return new SnapshotSummaryResult(userId, roundId, totalAssetKrw, totalInvestmentKrw);
    }
}
