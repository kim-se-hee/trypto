package ksh.tryptobackend.trading.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record Fill(Price filledPrice, Money fee, LocalDateTime filledAt) {

    public static Fill settle(
            Price filledPrice, Quantity quantity, BigDecimal feeRate, LocalDateTime filledAt) {
        Money fee = filledPrice.times(quantity).times(feeRate);
        return new Fill(filledPrice, fee, filledAt);
    }
}
