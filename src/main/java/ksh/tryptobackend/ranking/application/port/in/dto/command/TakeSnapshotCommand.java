package ksh.tryptobackend.ranking.application.port.in.dto.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TakeSnapshotCommand(
    Long roundId,
    Long userId,
    Long exchangeId,
    Long walletId,
    BigDecimal seedAmount,
    LocalDate snapshotDate
) {
}
