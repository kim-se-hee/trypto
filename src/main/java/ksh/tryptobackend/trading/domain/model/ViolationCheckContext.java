package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.trading.domain.vo.Side;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ViolationCheckContext(
    Side orderSide,
    BigDecimal changeRate,
    Holding holding,
    BigDecimal currentPrice,
    long todayOrderCount,
    LocalDateTime now
) {
}
