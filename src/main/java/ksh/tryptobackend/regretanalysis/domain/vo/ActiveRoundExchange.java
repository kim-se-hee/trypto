package ksh.tryptobackend.regretanalysis.domain.vo;

import java.time.LocalDateTime;

public record ActiveRoundExchange(
    Long roundId,
    Long userId,
    Long exchangeId,
    Long walletId,
    LocalDateTime startedAt
) {
}
