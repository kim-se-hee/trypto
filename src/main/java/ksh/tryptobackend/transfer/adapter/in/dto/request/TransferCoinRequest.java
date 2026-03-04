package ksh.tryptobackend.transfer.adapter.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import ksh.tryptobackend.transfer.application.port.in.dto.command.TransferCoinCommand;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCoinRequest(
    @NotNull UUID idempotencyKey,
    @NotNull Long fromWalletId,
    @NotNull Long coinId,
    @NotBlank String chain,
    @NotBlank String toAddress,
    String toTag,
    @NotNull @Positive BigDecimal amount
) {

    public TransferCoinCommand toCommand() {
        return new TransferCoinCommand(idempotencyKey, fromWalletId, coinId, chain, toAddress, toTag, amount);
    }
}
