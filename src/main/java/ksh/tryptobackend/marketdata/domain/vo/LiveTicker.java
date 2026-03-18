package ksh.tryptobackend.marketdata.domain.vo;

import java.math.BigDecimal;

public record LiveTicker(
    Long coinId,
    String symbol,
    BigDecimal price,
    BigDecimal changeRate,
    BigDecimal quoteTurnover,
    Long timestamp
) {
}
