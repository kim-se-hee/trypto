package ksh.tryptobackend.investmentround.application.port.in.dto.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CheckRuleViolationsQuery(
    Long walletId,
    boolean buyOrder,
    BigDecimal changeRate,
    HoldingState holdingState,
    BigDecimal currentPrice,
    long todayOrderCount,
    LocalDateTime now
) {
}
