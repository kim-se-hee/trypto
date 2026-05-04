package ksh.tryptobackend.trading.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import ksh.tryptobackend.trading.application.port.in.dto.command.CancelOrderCommand;

public record CancelOrderRequest(@NotNull Long walletId) {

    public CancelOrderCommand toCommand(Long orderId) {
        return new CancelOrderCommand(orderId, walletId);
    }
}
