package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SoldPortionResult(BigDecimal filledPrice, BigDecimal quantity, LocalDateTime filledAt) {}
