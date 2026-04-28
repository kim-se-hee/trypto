package ksh.tryptobackend.marketdata.domain.vo;

import java.math.BigDecimal;
import java.time.Instant;

public record Tick(
    Instant time,
    BigDecimal price
) {
}
