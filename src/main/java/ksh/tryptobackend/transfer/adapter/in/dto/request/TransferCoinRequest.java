package ksh.tryptobackend.transfer.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ksh.tryptobackend.transfer.application.port.in.dto.command.TransferCoinCommand;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCoinRequest(
    @NotNull UUID idempotencyKey,
    @NotNull Long fromWalletId,
    @NotNull Long toWalletId,
    @NotNull Long coinId,
    @NotNull @Positive BigDecimal amount
) {

    public TransferCoinCommand toCommand() {
        return new TransferCoinCommand(idempotencyKey, fromWalletId, toWalletId, coinId, amount);
    }
}
