package ksh.tryptobackend.wallet.application.port.in.dto.command;

import java.math.BigDecimal;

public record BalanceChangeItem(BalanceChangeType type, Long coinId, BigDecimal amount) {}
