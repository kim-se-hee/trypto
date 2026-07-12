package ksh.tryptobackend.wallet.application.port.in.dto.command;

import java.math.BigDecimal;

public record TransferCoinCommand(
        String idempotencyKey, Long fromWalletId, Long toWalletId, Long coinId, BigDecimal amount) {}
