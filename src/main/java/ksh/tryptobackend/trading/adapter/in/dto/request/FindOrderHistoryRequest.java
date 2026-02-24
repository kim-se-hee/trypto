package ksh.tryptobackend.trading.adapter.in.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.trading.application.port.in.dto.query.FindOrderHistoryQuery;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.Side;

public record FindOrderHistoryRequest(
    @NotNull Long walletId,
    Long exchangeCoinId,
    Side side,
    OrderStatus status,
    Long cursorOrderId,
    @Min(1) @Max(50) Integer size
) {

    public FindOrderHistoryRequest {
        if (size == null) {
            size = 20;
        }
    }

    public FindOrderHistoryQuery toQuery() {
        return new FindOrderHistoryQuery(walletId, exchangeCoinId, side, status, cursorOrderId, size);
    }
}
