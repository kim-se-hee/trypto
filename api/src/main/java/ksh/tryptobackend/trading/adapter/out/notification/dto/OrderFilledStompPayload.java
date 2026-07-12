package ksh.tryptobackend.trading.adapter.out.notification.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.trading.domain.vo.OrderFilledNotification;

public record OrderFilledStompPayload(
        String eventType, Long orderId, BigDecimal executedPrice, BigDecimal quantity, LocalDateTime executedAt) {

    private static final String EVENT_TYPE = "ORDER_FILLED";

    public static OrderFilledStompPayload from(OrderFilledNotification notification) {
        return new OrderFilledStompPayload(
                EVENT_TYPE,
                notification.orderId(),
                notification.executedPrice(),
                notification.quantity(),
                notification.executedAt());
    }
}
