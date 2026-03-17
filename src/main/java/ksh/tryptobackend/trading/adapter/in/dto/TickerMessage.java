package ksh.tryptobackend.trading.adapter.in.dto;

import java.math.BigDecimal;

public record TickerMessage(
    String exchange,
    String symbol,
    BigDecimal currentPrice,
    Long timestamp
) {
}
