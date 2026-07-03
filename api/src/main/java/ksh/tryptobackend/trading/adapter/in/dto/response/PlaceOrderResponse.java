package ksh.tryptobackend.trading.adapter.in.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;

public record PlaceOrderResponse(
        Long orderId,
        Side side,
        OrderType orderType,
        BigDecimal orderAmount,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal filledPrice,
        BigDecimal fee,
        OrderStatus status,
        LocalDateTime createdAt,
        LocalDateTime filledAt) {

    public static PlaceOrderResponse from(Order order) {
        return new PlaceOrderResponse(
                order.getId(),
                order.getSide(),
                order.getOrderType(),
                order.getFilledAmount() != null ? order.getFilledAmount().value() : null,
                order.getQuantity().value(),
                order.getLimitPrice() != null ? order.getLimitPrice().value() : null,
                order.getFilledPrice() != null ? order.getFilledPrice().value() : null,
                order.getFee() != null ? order.getFee().amount().value() : null,
                order.getStatus(),
                order.getCreatedAt(),
                order.getFilledAt());
    }
}
