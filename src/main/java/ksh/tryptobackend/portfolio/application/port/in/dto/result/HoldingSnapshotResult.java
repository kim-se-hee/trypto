package ksh.tryptobackend.portfolio.application.port.in.dto.result;

import java.math.BigDecimal;

public record HoldingSnapshotResult(
    Long coinId,
    String coinSymbol,
    String coinName,
    BigDecimal quantity,
    BigDecimal avgBuyPrice,
    BigDecimal currentPrice
) {
}
