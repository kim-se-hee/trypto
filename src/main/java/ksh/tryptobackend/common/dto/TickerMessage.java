package ksh.tryptobackend.common.dto;

import java.math.BigDecimal;

public record TickerMessage(
    String exchange,
    String symbol,
    BigDecimal currentPrice,
    BigDecimal changeRate,
    BigDecimal quoteTurnover,
    Long timestamp
) {
}
