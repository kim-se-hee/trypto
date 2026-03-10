package ksh.tryptobackend.portfolio.adapter.in.dto.response;

import ksh.tryptobackend.portfolio.application.port.in.dto.result.HoldingSnapshotResult;

import java.math.BigDecimal;

public record HoldingSnapshotResponse(
    Long coinId,
    String coinSymbol,
    String coinName,
    BigDecimal quantity,
    BigDecimal avgBuyPrice,
    BigDecimal currentPrice
) {

    public static HoldingSnapshotResponse from(HoldingSnapshotResult result) {
        return new HoldingSnapshotResponse(
                result.coinId(),
                result.coinSymbol(),
                result.coinName(),
                result.quantity(),
                result.avgBuyPrice(),
                result.currentPrice()
        );
    }
}
