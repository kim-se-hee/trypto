package ksh.tryptobackend.trading.adapter.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;

public record PlaceOrderRequest(
        @NotBlank String clientOrderId,
        @NotNull Long walletId,
        @NotNull Long exchangeCoinId,
        @NotNull Side side,
        @NotNull OrderType orderType,
        BigDecimal price,
        @NotNull @Positive BigDecimal amount) {

    public PlaceOrderCommand toCommand() {
        return new PlaceOrderCommand(
                clientOrderId, walletId, exchangeCoinId, side, orderType, price, amount);
    }
}
