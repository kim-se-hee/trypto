package ksh.tryptobackend.portfolio.adapter.in.dto.response;

import ksh.tryptobackend.portfolio.application.port.in.dto.result.MyHoldingsResult;

import java.math.BigDecimal;
import java.util.List;

public record MyHoldingsResponse(
    Long exchangeId,
    BigDecimal baseCurrencyBalance,
    String baseCurrencySymbol,
    List<HoldingSnapshotResponse> holdings
) {

    public static MyHoldingsResponse from(MyHoldingsResult result) {
        return new MyHoldingsResponse(
                result.exchangeId(),
                result.baseCurrencyBalance(),
                result.baseCurrencySymbol(),
                result.holdings().stream()
                        .map(HoldingSnapshotResponse::from)
                        .toList()
        );
    }
}
