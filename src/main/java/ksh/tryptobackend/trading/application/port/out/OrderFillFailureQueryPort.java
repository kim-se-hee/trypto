package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.model.OrderFillFailure;

import java.util.List;

public interface OrderFillFailureQueryPort {

    List<OrderFillFailure> findUnresolved();
}
