package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.math.BigDecimal;
import ksh.tryptobackend.trading.domain.model.Holding;

public record HoldingInfoResult(Long coinId, BigDecimal avgBuyPrice, BigDecimal totalQuantity) {

    public static HoldingInfoResult from(Holding holding) {
        return new HoldingInfoResult(
                holding.getCoinId(), holding.getAvgBuyPrice(), holding.getTotalQuantity());
    }
}
