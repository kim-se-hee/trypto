package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StartRoundResult(
    Long roundId,
    long roundNumber,
    RoundStatus status,
    BigDecimal initialSeed,
    BigDecimal emergencyFundingLimit,
    int emergencyChargeCount,
    List<StartRoundRuleResult> rules,
    LocalDateTime startedAt
) {
}
