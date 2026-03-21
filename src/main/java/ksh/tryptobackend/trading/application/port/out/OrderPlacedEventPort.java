package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.vo.OrderPlacedEvent;

public interface OrderPlacedEventPort {

    void publish(OrderPlacedEvent event);
}
