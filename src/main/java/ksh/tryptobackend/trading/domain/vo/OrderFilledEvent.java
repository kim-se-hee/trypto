package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;

public record OrderFilledEvent(
    Long userId,
    Long walletId,
    Long orderId,
    Long coinId,
    Long baseCoinId,
    Side side,
    BigDecimal quantity,
    BigDecimal price,
    BigDecimal currentPrice,
    BigDecimal fee
) {
}
