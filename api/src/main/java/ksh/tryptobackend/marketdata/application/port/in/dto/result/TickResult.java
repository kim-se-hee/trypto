package ksh.tryptobackend.marketdata.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.Instant;

public record TickResult(Instant time, BigDecimal price) {}
