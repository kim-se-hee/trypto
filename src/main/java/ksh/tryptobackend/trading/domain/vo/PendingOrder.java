package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;

public record PendingOrder(
    Long orderId,
    Long exchangeCoinId,
    Side side,
    BigDecimal price
) {

    public boolean matchesBuy(BigDecimal currentPrice) {
        return side == Side.BUY && currentPrice.compareTo(price) <= 0;
    }

    public boolean matchesSell(BigDecimal currentPrice) {
        return side == Side.SELL && currentPrice.compareTo(price) >= 0;
    }

    public boolean matches(BigDecimal currentPrice) {
        return side == Side.BUY ? matchesBuy(currentPrice) : matchesSell(currentPrice);
    }
}
