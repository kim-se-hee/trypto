package ksh.tryptobackend.trading.adapter.in.dto.response;

import java.math.BigDecimal;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderAvailabilityResult;

public record OrderAvailabilityResponse(BigDecimal available, BigDecimal currentPrice) {

    public static OrderAvailabilityResponse from(OrderAvailabilityResult result) {
        return new OrderAvailabilityResponse(result.available(), result.currentPrice());
    }
}
