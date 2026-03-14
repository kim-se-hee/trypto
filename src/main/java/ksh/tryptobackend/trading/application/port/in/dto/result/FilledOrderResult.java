package ksh.tryptobackend.trading.application.port.in.dto.result;

import ksh.tryptobackend.trading.application.port.out.dto.OrderInfo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FilledOrderResult(
    Long orderId,
    Long walletId,
    Long exchangeCoinId,
    String side,
    BigDecimal amount,
    BigDecimal quantity,
    BigDecimal filledPrice,
    LocalDateTime filledAt
) {

    public static FilledOrderResult from(OrderInfo info) {
        return new FilledOrderResult(
            info.orderId(), info.walletId(), info.exchangeCoinId(),
            info.side().name(), info.amount(), info.quantity(),
            info.filledPrice(), info.filledAt());
    }
}
