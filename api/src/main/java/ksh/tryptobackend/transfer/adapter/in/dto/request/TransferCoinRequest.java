package ksh.tryptobackend.transfer.adapter.in.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.util.UUID;
import ksh.tryptobackend.transfer.application.port.in.dto.command.TransferCoinCommand;

public record TransferCoinRequest(
        @NotNull UUID idempotencyKey,
        @NotNull Long fromWalletId,
        @NotNull Long toWalletId,
        @NotNull Long coinId,
        @NotNull @Positive BigDecimal amount) {

    public TransferCoinCommand toCommand() {
        return new TransferCoinCommand(idempotencyKey, fromWalletId, toWalletId, coinId, amount);
    }
}
