package ksh.tryptobackend.trading.application.port.in.dto.command;

import java.math.BigDecimal;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;

public record PlaceOrderCommand(
        String idempotencyKey,
        Long requesterId,
        Long walletId,
        Long exchangeCoinId,
        Side side,
        OrderType orderType,
        BigDecimal volume,
        BigDecimal price) {}
