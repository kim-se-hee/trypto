package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrphanOrder(
        Long orderId,
        Long walletId,
        Long exchangeCoinId,
        Side side,
        BigDecimal price,
        BigDecimal quantity,
        LocalDateTime createdAt) {

    public boolean matches(BigDecimal candidatePrice) {
        return side.canFillAt(Price.of(price), Price.of(candidatePrice));
    }
}
