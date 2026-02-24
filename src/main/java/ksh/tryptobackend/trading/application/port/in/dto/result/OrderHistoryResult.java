package ksh.tryptobackend.trading.application.port.in.dto.result;

import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderHistoryResult(
    Long orderId,
    Long exchangeCoinId,
    Side side,
    OrderType orderType,
    BigDecimal filledPrice,
    BigDecimal price,
    BigDecimal quantity,
    BigDecimal orderAmount,
    BigDecimal fee,
    LocalDateTime createdAt,
    LocalDateTime filledAt
) {

    public static OrderHistoryResult from(Order order) {
        return new OrderHistoryResult(
            order.getId(),
            order.getExchangeCoinId(),
            order.getSide(),
            order.getOrderType(),
            order.getFilledPrice(),
            order.getPrice(),
            order.getQuantity(),
            order.getOrderAmount(),
            order.getFee() != null ? order.getFee().getAmount() : null,
            order.getCreatedAt(),
            order.getFilledAt()
        );
    }
}
