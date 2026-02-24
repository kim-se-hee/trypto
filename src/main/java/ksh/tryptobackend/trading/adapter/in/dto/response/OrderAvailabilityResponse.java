package ksh.tryptobackend.trading.adapter.in.dto.response;

import ksh.tryptobackend.trading.application.port.in.dto.result.OrderAvailabilityResult;

import java.math.BigDecimal;

public record OrderAvailabilityResponse(
    BigDecimal available,
    BigDecimal currentPrice
) {

    public static OrderAvailabilityResponse from(OrderAvailabilityResult result) {
        return new OrderAvailabilityResponse(result.available(), result.currentPrice());
    }
}
