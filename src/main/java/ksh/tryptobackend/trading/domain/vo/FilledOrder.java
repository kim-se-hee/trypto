package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FilledOrder(
    Long orderId,
    Long walletId,
    Long exchangeCoinId,
    Side side,
    BigDecimal amount,
    BigDecimal quantity,
    BigDecimal filledPrice,
    LocalDateTime filledAt
) {
}
