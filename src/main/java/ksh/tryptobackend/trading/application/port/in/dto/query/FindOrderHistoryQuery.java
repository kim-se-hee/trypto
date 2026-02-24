package ksh.tryptobackend.trading.application.port.in.dto.query;

import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.Side;

public record FindOrderHistoryQuery(
    Long walletId,
    Long exchangeCoinId,
    Side side,
    OrderStatus status,
    Long cursorOrderId,
    int size
) {
}
