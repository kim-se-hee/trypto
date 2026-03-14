package ksh.tryptobackend.investmentround.application.port.in.dto.query;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CheckRuleViolationsQuery(
    Long walletId,
    boolean buyOrder,
    BigDecimal changeRate,
    BigDecimal avgBuyPrice,
    BigDecimal totalQuantity,
    int averagingDownCount,
    BigDecimal currentPrice,
    long todayOrderCount,
    LocalDateTime now
) {
}
