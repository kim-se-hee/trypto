package ksh.tryptobackend.wallet.application.port.in.dto.command;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CreateWalletWithBalanceCommand(
    Long roundId,
    Long exchangeId,
    Long baseCurrencyCoinId,
    BigDecimal initialAmount,
    LocalDateTime createdAt
) {
}
