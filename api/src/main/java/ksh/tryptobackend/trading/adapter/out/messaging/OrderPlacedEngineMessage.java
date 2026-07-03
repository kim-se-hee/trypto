package ksh.tryptobackend.trading.adapter.out.messaging;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;

public record OrderPlacedEngineMessage(
        Long orderId,
        Long walletId,
        String side,
        Long exchangeCoinId,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal lockedAmount,
        Long lockedCoinId,
        LocalDateTime placedAt) {
    public static OrderPlacedEngineMessage from(OrderPlacedEvent event) {
        return new OrderPlacedEngineMessage(
                event.orderId(),
                event.walletId(),
                event.side().name(),
                event.exchangeCoinId(),
                event.limitPrice(),
                event.quantity(),
                event.lockAmount(),
                event.lockedCoinId(),
                event.createdAt());
    }
}
