package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.domain.event.OrderFilledEvent;

public interface SettleOrderUseCase {

    void settle(OrderFilledEvent event);
}
