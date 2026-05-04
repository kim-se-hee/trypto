package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RoundInfoResult(
        Long roundId,
        Long userId,
        long roundNumber,
        BigDecimal initialSeed,
        BigDecimal emergencyFundingLimit,
        int emergencyChargeCount,
        String status,
        LocalDateTime startedAt,
        LocalDateTime endedAt) {}
