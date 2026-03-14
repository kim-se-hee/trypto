package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.math.BigDecimal;

public record EvaluatedHoldingResult(
    Long coinId,
    BigDecimal avgBuyPrice,
    BigDecimal totalQuantity,
    BigDecimal currentPrice
) {
}
