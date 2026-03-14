package ksh.tryptobackend.portfolio.domain.vo;

import java.math.BigDecimal;

public record HoldingSnapshot(
    Long coinId,
    String symbol,
    String name,
    BigDecimal quantity,
    BigDecimal avgBuyPrice,
    BigDecimal currentPrice
) {
}
