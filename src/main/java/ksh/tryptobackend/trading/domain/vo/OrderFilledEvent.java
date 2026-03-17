package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;

public record OrderFilledEvent(
    Long userId,
    Long walletId,
    Long orderId,
    Long coinId,
    Side side,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal fee
) {
}
