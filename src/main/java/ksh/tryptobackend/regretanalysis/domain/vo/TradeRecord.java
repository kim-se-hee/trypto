package ksh.tryptobackend.regretanalysis.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradeRecord(
    Long orderId,
    Long walletId,
    Long exchangeCoinId,
    TradeSide side,
    BigDecimal amount,
    BigDecimal quantity,
    BigDecimal filledPrice,
    LocalDateTime filledAt
) {

    public boolean isBuy() {
        return side == TradeSide.BUY;
    }

    public boolean isSell() {
        return side == TradeSide.SELL;
    }

    public BigDecimal getTradeValue() {
        return filledPrice.multiply(quantity);
    }
}
