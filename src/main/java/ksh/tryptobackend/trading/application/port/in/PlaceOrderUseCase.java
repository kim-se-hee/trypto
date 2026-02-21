package ksh.tryptobackend.trading.application.port.in;

import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.domain.model.Order;

public interface PlaceOrderUseCase {

    Order placeOrder(PlaceOrderCommand command);
}
