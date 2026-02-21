package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.adapter.in.dto.query.GetOrderAvailabilityQuery;
import ksh.tryptobackend.trading.adapter.in.dto.response.OrderAvailabilityResponse;

public interface GetOrderAvailabilityUseCase {

    OrderAvailabilityResponse getAvailability(GetOrderAvailabilityQuery query);
}
