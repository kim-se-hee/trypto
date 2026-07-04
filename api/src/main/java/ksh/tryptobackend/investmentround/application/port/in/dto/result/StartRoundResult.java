package ksh.tryptobackend.investmentround.application.port.in.dto.result;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;

public record StartRoundResult(
        Long roundId,
        long roundNumber,
        RoundStatus status,
        BigDecimal initialSeed,
        BigDecimal emergencyFundingLimit,
        int emergencyChargeCount,
        List<StartRoundRuleResult> rules,
        List<StartRoundWalletResult> wallets,
        LocalDateTime startedAt) {

    public static StartRoundResult from(
            InvestmentRound round, List<StartRoundWalletResult> wallets) {
        List<StartRoundRuleResult> ruleResults =
                round.getRules().rules().stream().map(StartRoundRuleResult::from).toList();

        return new StartRoundResult(
                round.getId(),
                round.getRoundNumber(),
                round.getStatus(),
                round.getInitialSeed(),
                round.getEmergencyFundingLimit(),
                round.getEmergencyChargeCount(),
                ruleResults,
                wallets,
                round.getStartedAt());
    }
}
