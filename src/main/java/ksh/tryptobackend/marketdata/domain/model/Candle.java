package ksh.tryptobackend.marketdata.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Candle(
    Instant time,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close
) {
}
