package ksh.tryptobackend.ranking.application.port.in.dto.result;

import java.math.BigDecimal;

public record PortfolioHoldingResult(
    String coinSymbol,
    String exchangeName,
    BigDecimal assetRatio,
    BigDecimal profitRate
) {
}
