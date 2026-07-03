package ksh.tryptobackend.trading.adapter.in.messages;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EngineOrderFilledMessage(
        Long orderId,
        BigDecimal executedPrice,
        BigDecimal quantity,
        LocalDateTime executedAt,
        LocalDateTime matchedAt) {}
