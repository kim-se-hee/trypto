package ksh.tryptobackend.transfer.application.port.in.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferCoinCommand(
    UUID idempotencyKey,
    Long fromWalletId,
    Long coinId,
    String chain,
    String toAddress,
    String toTag,
    BigDecimal amount
) {
}
