package ksh.tryptobackend.investmentround.adapter.in.dto.response;

import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundRuleResult;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import ksh.tryptobackend.trading.domain.vo.RuleType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record StartRoundResponse(
    Long roundId,
    long roundNumber,
    RoundStatus status,
    BigDecimal initialSeed,
    BigDecimal emergencyFundingLimit,
    int emergencyChargeCount,
    List<RuleResponse> rules,
    LocalDateTime startedAt
) {

    public static StartRoundResponse from(StartRoundResult result) {
        return new StartRoundResponse(
            result.roundId(),
            result.roundNumber(),
            result.status(),
            result.initialSeed(),
            result.emergencyFundingLimit(),
            result.emergencyChargeCount(),
            result.rules().stream().map(RuleResponse::from).toList(),
            result.startedAt()
        );
    }

    public record RuleResponse(
        Long ruleId,
        RuleType ruleType,
        BigDecimal thresholdValue
    ) {

        public static RuleResponse from(StartRoundRuleResult result) {
            return new RuleResponse(result.ruleId(), result.ruleType(), result.thresholdValue());
        }
    }
}
