package ksh.tryptobackend.regretanalysis.application.port.out.dto;

import java.time.LocalDateTime;

public record RoundExchangeInfo(
    Long roundId,
    Long userId,
    Long exchangeId,
    Long walletId,
    LocalDateTime startedAt
) {
}
