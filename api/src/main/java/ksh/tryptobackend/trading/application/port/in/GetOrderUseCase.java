package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.domain.model.Order;

public interface GetOrderUseCase {

    Order getById(Long orderId);
}
