package ksh.tryptobackend.investmentround.application.port.in.dto.query;

import java.math.BigDecimal;

public record HoldingState(
    BigDecimal avgBuyPrice,
    BigDecimal totalQuantity,
    int averagingDownCount
) {

    public boolean isHolding() {
        return totalQuantity != null && totalQuantity.compareTo(BigDecimal.ZERO) > 0;
    }

    public boolean isAtLoss(BigDecimal currentPrice) {
        return isHolding() && avgBuyPrice.compareTo(currentPrice) > 0;
    }
}
