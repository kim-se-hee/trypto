package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.application.port.in.dto.command.CancelOrderCommand;
import ksh.tryptobackend.trading.domain.model.Order;

public interface CancelOrderUseCase {

    Order cancelOrder(CancelOrderCommand command);
}
