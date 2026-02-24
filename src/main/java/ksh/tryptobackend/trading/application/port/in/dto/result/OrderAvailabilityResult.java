package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.math.BigDecimal;

public record OrderAvailabilityResult(
    BigDecimal available,
    BigDecimal currentPrice
) {
}
