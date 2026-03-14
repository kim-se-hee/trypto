package ksh.tryptobackend.investmentround.adapter.in.dto.response;

import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundRuleResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.StartRoundWalletResult;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import ksh.tryptobackend.common.domain.vo.RuleType;

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
    List<WalletResponse> wallets,
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
            result.wallets().stream().map(WalletResponse::from).toList(),
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

    public record WalletResponse(Long walletId, Long exchangeId) {

        public static WalletResponse from(StartRoundWalletResult result) {
            return new WalletResponse(result.walletId(), result.exchangeId());
        }
    }
}
