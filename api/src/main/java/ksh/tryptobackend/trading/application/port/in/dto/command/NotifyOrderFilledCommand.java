package ksh.tryptobackend.trading.application.port.in.dto.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NotifyOrderFilledCommand(
        Long orderId,
        Long userId,
        BigDecimal executedPrice,
        BigDecimal quantity,
        LocalDateTime executedAt,
        LocalDateTime matchedAt) {}
