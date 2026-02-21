package ksh.tryptobackend.trading.application.port.in.dto.command;

import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceOrderCommand(
        UUID idempotencyKey,
        Long walletId,
        Long exchangeCoinId,
        Side side,
        OrderType orderType,
        BigDecimal price,
        BigDecimal amount
) {
}
