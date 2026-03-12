package ksh.tryptobackend.marketdata.adapter.in.dto;

import java.math.BigDecimal;

public record LivePriceMessage(
        Long coinId,
        String symbol,
        BigDecimal price,
        BigDecimal changeRate,
        Long timestamp
) {}
