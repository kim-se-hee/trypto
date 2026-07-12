package ksh.tryptobackend.portfolio.application.port.in.dto.result;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.portfolio.domain.model.Portfolio;

public record MyHoldingsResult(
        Long exchangeId,
        BigDecimal baseCurrencyBalance,
        String baseCurrencySymbol,
        List<HoldingSnapshotResult> holdings) {

    public static MyHoldingsResult from(Portfolio portfolio) {
        return new MyHoldingsResult(
                portfolio.exchangeId(),
                portfolio.baseCurrencyBalance(),
                portfolio.baseCurrencySymbol(),
                portfolio.holdingSnapshots().stream()
                        .map(HoldingSnapshotResult::from)
                        .toList());
    }
}
