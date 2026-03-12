package ksh.tryptobackend.marketdata.adapter.in.dto.response;

import java.math.BigDecimal;

public record LivePriceResponse(
        Long coinId,
        String symbol,
        BigDecimal price,
        BigDecimal changeRate,
        Long timestamp
) {}
