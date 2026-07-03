package ksh.tryptoengine.consumer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderPlacedEvent(
        Long orderId,
        Long walletId,
        String side,
        Long exchangeCoinId,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal lockedAmount,
        Long lockedCoinId,
        LocalDateTime placedAt)
        implements EngineInboundEvent {}
