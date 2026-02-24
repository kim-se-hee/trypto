package ksh.tryptobackend.trading.adapter.in.dto.response;

import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
    LocalDateTime filledAt
) {

    public static PlaceOrderResponse from(Order order) {
        return new PlaceOrderResponse(
            order.getId(),
            order.getSide(),
            order.getOrderType(),
            order.getOrderAmount(),
            order.getQuantity(),
            order.getPrice(),
            order.getFilledPrice(),
            order.getFee() != null ? order.getFee().getAmount() : null,
            order.getStatus(),
            order.getCreatedAt(),
            order.getFilledAt()
        );
    }
}
