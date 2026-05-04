package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;

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
        LocalDateTime filledAt) {

    public static OrderHistoryResult from(Order order) {
        return new OrderHistoryResult(
                order.getId(),
                order.getExchangeCoinId(),
                order.getSide(),
                order.getOrderType(),
                order.getFilledPrice(),
                order.getPrice(),
                order.getQuantity().value(),
                order.getAmount(),
                order.getFee() != null ? order.getFee().amount() : null,
                order.getCreatedAt(),
                order.getFilledAt());
    }
}
