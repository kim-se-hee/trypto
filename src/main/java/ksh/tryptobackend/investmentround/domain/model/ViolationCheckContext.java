package ksh.tryptobackend.investmentround.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ViolationCheckContext(
    boolean buyOrder,
    BigDecimal changeRate,
    BigDecimal avgBuyPrice,
    BigDecimal totalQuantity,
    int averagingDownCount,
    BigDecimal currentPrice,
    long todayOrderCount,
    LocalDateTime now
) {

    public boolean isHolding() {
        return totalQuantity != null && totalQuantity.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isAtLoss() {
        return isHolding() && avgBuyPrice.compareTo(currentPrice) > 0;
    }
}
