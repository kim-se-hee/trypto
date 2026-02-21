package ksh.tryptobackend.trading.adapter.in.dto.query;

import ksh.tryptobackend.trading.domain.vo.Side;

public record GetOrderAvailabilityQuery(
        Long walletId,
        Long exchangeCoinId,
        Side side
) {
}
