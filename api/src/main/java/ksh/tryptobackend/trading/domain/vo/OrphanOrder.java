package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrphanOrder(
    Long orderId,
    Long walletId,
    Long exchangeCoinId,
    Long coinId,
    Long baseCoinId,
    String exchangeName,
    String marketSymbol,
    Side side,
    BigDecimal price,
    BigDecimal quantity,
    BigDecimal amount,
    LocalDateTime createdAt
) {

    public boolean matches(BigDecimal candidatePrice) {
        return isBuy()
            ? candidatePrice.compareTo(price) <= 0
            : candidatePrice.compareTo(price) >= 0;
    }

    public boolean isBuy() {
        return side == Side.BUY;
    }

    public Long lockedCoinId() {
        return isBuy() ? baseCoinId : coinId;
    }

    public BigDecimal lockedAmount() {
        return isBuy() ? amount : quantity;
    }
}
