package ksh.tryptobackend.trading.adapter.out.dto;

import ksh.tryptobackend.trading.domain.vo.OrderFilledEvent;

import java.math.BigDecimal;

public record OrderFilledMessage(
    String eventType,
    Long walletId,
    Long orderId,
    Long coinId,
    String side,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal fee
) {

    public static OrderFilledMessage from(OrderFilledEvent event) {
        return new OrderFilledMessage(
            "ORDER_FILLED",
            event.walletId(),
            event.orderId(),
            event.coinId(),
            event.side().name(),
            event.quantity(),
            event.price(),
            event.fee());
    }
}
