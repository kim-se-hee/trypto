package ksh.tryptoengine.outbox;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderFilledEvent(
    Long orderId,
    Long userId,
    BigDecimal executedPrice,
    BigDecimal quantity,
    LocalDateTime executedAt
) {
}
