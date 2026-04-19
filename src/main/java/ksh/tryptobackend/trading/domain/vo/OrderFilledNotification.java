package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderFilledNotification(
    Long orderId,
    Long userId,
    BigDecimal executedPrice,
    BigDecimal quantity,
    LocalDateTime executedAt
) {
}
