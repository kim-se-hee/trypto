package ksh.tryptobackend.trading.application.port.in.dto.result;

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
}
