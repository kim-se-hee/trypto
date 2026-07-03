package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.model.Order;

public interface OrderCommandPort {

    Order save(Order order);
}
