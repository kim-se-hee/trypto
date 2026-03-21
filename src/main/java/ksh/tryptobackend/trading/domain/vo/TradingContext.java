package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TradingContext(
    Long coinId,
    TradingVenue venue,
    OrderMode mode,
    BigDecimal currentPrice,
    LocalDateTime now
) {

    public Long balanceCoinId() {
        return mode.resolveBalanceCoinId(venue, coinId);
    }
}
