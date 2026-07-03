package ksh.tryptobackend.trading.adapter.out.messaging;

import ksh.tryptobackend.trading.domain.model.Order;

public record OrderCanceledEngineMessage(Long orderId, Long exchangeCoinId) {
    public static OrderCanceledEngineMessage from(Order order) {
        return new OrderCanceledEngineMessage(order.getId(), order.getExchangeCoinId());
    }
}
