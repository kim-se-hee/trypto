package ksh.tryptobackend.regretanalysis.application.port.in.dto.result;

import java.time.LocalDateTime;

public record RegretReportInputResult(
    Long roundId,
    Long userId,
    Long exchangeId,
    Long walletId,
    LocalDateTime startedAt
) {
}
