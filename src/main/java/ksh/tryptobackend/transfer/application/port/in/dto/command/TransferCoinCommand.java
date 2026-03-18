package ksh.tryptobackend.transfer.application.port.in.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCoinCommand(
    UUID idempotencyKey,
    Long fromWalletId,
    Long toWalletId,
    Long coinId,
    BigDecimal amount
) {
}
