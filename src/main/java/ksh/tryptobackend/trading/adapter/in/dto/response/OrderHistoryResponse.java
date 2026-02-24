package ksh.tryptobackend.trading.adapter.in.dto.response;

import ksh.tryptobackend.trading.application.port.in.dto.result.OrderHistoryResult;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderHistoryResponse(
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

    public static OrderHistoryResponse from(OrderHistoryResult result) {
        return new OrderHistoryResponse(
            result.orderId(),
            result.exchangeCoinId(),
            result.side(),
            result.orderType(),
            result.filledPrice(),
            result.price(),
            result.quantity(),
            result.orderAmount(),
            result.fee(),
            result.createdAt(),
            result.filledAt()
        );
    }
}
