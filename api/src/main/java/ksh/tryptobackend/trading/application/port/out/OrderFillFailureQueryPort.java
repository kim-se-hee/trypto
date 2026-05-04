package ksh.tryptobackend.trading.application.port.out;

import java.util.List;
import ksh.tryptobackend.trading.domain.model.OrderFillFailure;

public interface OrderFillFailureQueryPort {

    List<OrderFillFailure> findUnresolved();
}
