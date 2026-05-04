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
                order.getAmount(),
                order.getQuantity().value(),
                order.getPrice(),
                order.getFilledPrice(),
                order.getFee() != null ? order.getFee().amount() : null,
                order.getStatus(),
                order.getCreatedAt(),
                order.getFilledAt());
    }
}
