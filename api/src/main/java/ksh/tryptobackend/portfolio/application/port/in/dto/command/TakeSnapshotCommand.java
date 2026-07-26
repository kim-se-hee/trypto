package ksh.tryptobackend.portfolio.application.port.in.dto.command;

import java.time.LocalDate;

public record TakeSnapshotCommand(Long roundId, Long userId, Long exchangeId, Long walletId, LocalDate snapshotDate) {}
