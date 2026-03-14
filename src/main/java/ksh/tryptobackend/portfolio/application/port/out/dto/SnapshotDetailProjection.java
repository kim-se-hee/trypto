package ksh.tryptobackend.portfolio.application.port.out.dto;

import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotDetailResult;

import java.math.BigDecimal;

public record SnapshotDetailProjection(
    Long coinId,
    Long exchangeId,
    BigDecimal assetRatio,
    BigDecimal profitRate
) {

    public SnapshotDetailResult toResult() {
        return new SnapshotDetailResult(coinId, exchangeId, assetRatio, profitRate);
    }
}
