package ksh.tryptobackend.trading.application.port.in.dto.result;

import ksh.tryptobackend.trading.domain.model.Holding;

import java.math.BigDecimal;

public record HoldingInfoResult(Long coinId, BigDecimal avgBuyPrice, BigDecimal totalQuantity) {

    public static HoldingInfoResult from(Holding holding) {
        return new HoldingInfoResult(holding.getCoinId(), holding.getAvgBuyPrice(), holding.getTotalQuantity());
    }
}
