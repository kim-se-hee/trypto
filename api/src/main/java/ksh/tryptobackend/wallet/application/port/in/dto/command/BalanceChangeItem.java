package ksh.tryptobackend.wallet.application.port.in.dto.command;

import java.math.BigDecimal;
import ksh.tryptobackend.wallet.application.port.in.dto.BalanceChangeType;

public record BalanceChangeItem(BalanceChangeType type, Long coinId, BigDecimal amount) {}
