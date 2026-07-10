package ksh.tryptobackend.trading.adapter.in.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderFilledEngineMessage(
        Long orderId,
        BigDecimal executedPrice,
        BigDecimal quantity,
        LocalDateTime executedAt,
        LocalDateTime matchedAt) {}
