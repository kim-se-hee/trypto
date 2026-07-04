package ksh.tryptobackend.trading.domain.event;

import ksh.tryptobackend.trading.domain.model.Order;

public record OrderCanceledEvent(Long orderId, Long exchangeCoinId) {

    public static OrderCanceledEvent of(Order order) {
        return new OrderCanceledEvent(order.getId(), order.getExchangeCoinId());
    }
}
