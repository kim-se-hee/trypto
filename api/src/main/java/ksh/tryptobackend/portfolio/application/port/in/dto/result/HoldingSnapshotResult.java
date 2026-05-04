package ksh.tryptobackend.portfolio.application.port.in.dto.result;

import java.math.BigDecimal;
import ksh.tryptobackend.portfolio.domain.vo.HoldingSnapshot;

public record HoldingSnapshotResult(
        Long coinId,
        String coinSymbol,
        String coinName,
        BigDecimal quantity,
        BigDecimal avgBuyPrice,
        BigDecimal currentPrice) {

    public static HoldingSnapshotResult from(HoldingSnapshot snapshot) {
        return new HoldingSnapshotResult(
                snapshot.coinId(),
                snapshot.symbol(),
                snapshot.name(),
                snapshot.quantity(),
                snapshot.avgBuyPrice(),
                snapshot.currentPrice());
    }
}
