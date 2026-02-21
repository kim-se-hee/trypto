package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.application.port.in.dto.query.GetOrderAvailabilityQuery;
import ksh.tryptobackend.trading.application.port.in.dto.result.OrderAvailabilityResult;

public interface GetOrderAvailabilityUseCase {

    OrderAvailabilityResult getAvailability(GetOrderAvailabilityQuery query);
}
