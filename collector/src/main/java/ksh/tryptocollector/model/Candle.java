package ksh.tryptocollector.model;

import java.math.BigDecimal;

public record Candle(
        String exchange,
        String symbol,
        long startMs,
        BigDecimal open,
        BigDecimal high,
        BigDecimal low,
        BigDecimal close) {}
