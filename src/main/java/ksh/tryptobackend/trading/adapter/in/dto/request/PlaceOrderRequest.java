package ksh.tryptobackend.trading.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;

import java.math.BigDecimal;
import java.util.UUID;

public record PlaceOrderRequest(
        @NotNull UUID clientOrderId,
        @NotNull Long walletId,
        @NotNull Long exchangeCoinId,
        @NotNull Side side,
        @NotNull OrderType orderType,
        BigDecimal price,
        @NotNull @Positive BigDecimal amount
) {

    public PlaceOrderCommand toCommand() {
        return new PlaceOrderCommand(
                clientOrderId, walletId, exchangeCoinId,
                side, orderType, price, amount
        );
    }
}
