package ksh.tryptobackend.marketdata.domain.vo;

import java.math.BigDecimal;

public record TickerSnapshot(BigDecimal price, BigDecimal changeRate, BigDecimal volume) {

    public static final TickerSnapshot EMPTY = new TickerSnapshot(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
}
