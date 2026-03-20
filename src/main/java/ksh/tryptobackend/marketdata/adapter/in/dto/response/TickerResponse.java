package ksh.tryptobackend.marketdata.adapter.in.dto.response;

import java.math.BigDecimal;

public record TickerResponse(
        Long coinId,
        String symbol,
        BigDecimal price,
        BigDecimal changeRate,
        BigDecimal quoteTurnover,
        Long timestamp
) {}
