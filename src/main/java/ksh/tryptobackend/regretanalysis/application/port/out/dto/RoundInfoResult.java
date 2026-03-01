package ksh.tryptobackend.regretanalysis.application.port.out.dto;

import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoundInfoResult(
    Long roundId,
    Long userId,
    BigDecimal initialSeed,
    RoundStatus status,
    LocalDateTime startedAt,
    LocalDateTime endedAt
) {
}
