package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceCandidate(Instant time, BigDecimal price) {
}
