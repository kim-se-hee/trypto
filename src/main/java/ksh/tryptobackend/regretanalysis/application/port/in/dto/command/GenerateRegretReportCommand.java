package ksh.tryptobackend.regretanalysis.application.port.in.dto.command;

import java.time.LocalDateTime;

public record GenerateRegretReportCommand(
    Long roundId,
    Long userId,
    Long exchangeId,
    Long walletId,
    LocalDateTime startedAt
) {
}
