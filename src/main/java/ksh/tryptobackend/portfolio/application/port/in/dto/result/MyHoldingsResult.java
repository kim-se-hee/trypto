package ksh.tryptobackend.portfolio.application.port.in.dto.result;

import java.math.BigDecimal;
import java.util.List;

public record MyHoldingsResult(
    Long exchangeId,
    BigDecimal baseCurrencyBalance,
    String baseCurrencySymbol,
    List<HoldingSnapshotResult> holdings
) {
}
