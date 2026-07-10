package ksh.tryptobackend.trading.adapter.out.messaging;

import ksh.tryptobackend.trading.domain.event.OrderCanceledEvent;

public record OrderCanceledEngineMessage(Long orderId, Long exchangeCoinId) {
    public static OrderCanceledEngineMessage from(OrderCanceledEvent event) {
        return new OrderCanceledEngineMessage(event.orderId(), event.exchangeCoinId());
    }
}
