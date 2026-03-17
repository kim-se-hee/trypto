package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.vo.OrderFilledEvent;

public interface OrderFilledEventPort {

    void publish(OrderFilledEvent event);
}
