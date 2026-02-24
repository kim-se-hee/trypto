package ksh.tryptobackend.trading.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.trading.application.port.in.dto.query.GetOrderAvailabilityQuery;
import ksh.tryptobackend.trading.domain.vo.Side;

public record GetOrderAvailabilityRequest(
    @NotNull Long walletId,
    @NotNull Long exchangeCoinId,
    @NotNull Side side
) {

    public GetOrderAvailabilityQuery toQuery() {
        return new GetOrderAvailabilityQuery(walletId, exchangeCoinId, side);
    }
}
