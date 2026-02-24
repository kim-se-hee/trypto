package ksh.tryptobackend.trading.adapter.in.dto.response;

import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;

public record CancelOrderResponse(
    Long orderId,
    OrderStatus status
) {

    public static CancelOrderResponse from(Order order) {
        return new CancelOrderResponse(order.getId(), order.getStatus());
    }
}
