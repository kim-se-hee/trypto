package ksh.tryptobackend.trading.adapter.in.dto.response;

import java.math.BigDecimal;

public record OrderAvailabilityResponse(
        BigDecimal available,
        BigDecimal currentPrice
) {
}
