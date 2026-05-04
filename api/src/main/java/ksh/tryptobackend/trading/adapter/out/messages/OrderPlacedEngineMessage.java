package ksh.tryptobackend.trading.adapter.out.messages;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.trading.domain.model.Order;

public record OrderPlacedEngineMessage(
        Long orderId,
        Long userId,
        Long walletId,
        String side,
        Long exchangeCoinId,
        Long coinId,
        Long baseCoinId,
        BigDecimal price,
        BigDecimal quantity,
        BigDecimal lockedAmount,
        Long lockedCoinId,
        LocalDateTime placedAt) {
    public static OrderPlacedEngineMessage from(Order order) {
        return new OrderPlacedEngineMessage(
                order.getId(),
                order.getUserId(),
                order.getWalletId(),
                order.getSide().name(),
                order.getExchangeCoinId(),
                order.getCoinId(),
                order.getBaseCoinId(),
                order.getPrice(),
                order.getQuantity().value(),
                order.getLockedAmount(),
                order.getLockedCoinId(),
                order.getCreatedAt());
    }
}
