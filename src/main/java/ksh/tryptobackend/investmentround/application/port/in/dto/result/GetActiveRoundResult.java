package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import ksh.tryptobackend.investmentround.domain.vo.RoundOverview;
import ksh.tryptobackend.investmentround.domain.model.RuleSetting;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record GetActiveRoundResult(
    Long roundId,
    Long userId,
    long roundNumber,
    RoundStatus status,
    BigDecimal initialSeed,
    BigDecimal emergencyFundingLimit,
    int emergencyChargeCount,
    LocalDateTime startedAt,
    LocalDateTime endedAt,
    List<GetActiveRoundRuleResult> rules
) {

    public static GetActiveRoundResult from(RoundOverview round, List<RuleSetting> rules) {
        List<GetActiveRoundRuleResult> ruleResults = rules.stream()
            .map(GetActiveRoundRuleResult::from)
            .toList();

        return new GetActiveRoundResult(
            round.roundId(),
            round.userId(),
            round.roundNumber(),
            round.status(),
            round.initialSeed(),
            round.emergencyFundingLimit(),
            round.emergencyChargeCount(),
            round.startedAt(),
            round.endedAt(),
            ruleResults
        );
    }
}
