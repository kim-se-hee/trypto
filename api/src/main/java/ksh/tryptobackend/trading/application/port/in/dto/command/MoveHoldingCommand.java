package ksh.tryptobackend.trading.application.port.in.dto.command;

import java.math.BigDecimal;

public record MoveHoldingCommand(
        Long fromWalletId, Long toWalletId, Long toExchangeId, Long coinId, BigDecimal amount) {}
