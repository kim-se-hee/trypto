package ksh.tryptobackend.portfolio.domain.vo;

import java.math.BigDecimal;

public record HoldingSummary(
    Long coinId,
    Long exchangeId,
    BigDecimal assetRatio,
    BigDecimal profitRate
) {
}
