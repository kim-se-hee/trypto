package ksh.tryptobackend.ranking.application.port.out.dto;

import java.math.BigDecimal;

public record SnapshotDetailProjection(
    String coinSymbol,
    String exchangeName,
    BigDecimal assetRatio,
    BigDecimal profitRate
) {
}
