package ksh.tryptobackend.trading.application.port.in.dto.result;

import java.math.BigDecimal;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.vo.Holding;

public record HoldingInfoResult(Long coinId, BigDecimal avgBuyPrice, BigDecimal totalQuantity) {

    public static HoldingInfoResult from(Position position) {
        Holding holding = position.getHolding();
        return new HoldingInfoResult(
                position.getCoinId(),
                holding.avgBuyPrice().value(),
                holding.totalQuantity().value());
    }
}
