package ksh.tryptobackend.marketdata.application.port.in.dto.result;

import java.math.BigDecimal;

public record LiveTickerResult(
    Long exchangeId,
    Long coinId,
    String symbol,
    BigDecimal price,
    BigDecimal changeRate,
    BigDecimal quoteTurnover,
    Long timestamp
) {}
