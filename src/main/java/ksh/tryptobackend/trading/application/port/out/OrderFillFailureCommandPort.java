package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.model.OrderFillFailure;

public interface OrderFillFailureCommandPort {

    OrderFillFailure save(OrderFillFailure failure);
}
