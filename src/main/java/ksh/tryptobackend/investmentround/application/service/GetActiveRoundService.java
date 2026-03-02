package ksh.tryptobackend.investmentround.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.GetActiveRoundUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.query.GetActiveRoundQuery;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.GetActiveRoundResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.GetActiveRoundRuleResult;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.InvestmentRuleQueryPort;
import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRoundInfo;
import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRuleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetActiveRoundService implements GetActiveRoundUseCase {

    private final InvestmentRoundQueryPort investmentRoundQueryPort;
    private final InvestmentRuleQueryPort investmentRuleQueryPort;

    @Override
    @Transactional(readOnly = true)
    public GetActiveRoundResult getActiveRound(GetActiveRoundQuery query) {
        InvestmentRoundInfo round = getActiveRound(query.userId());
        List<GetActiveRoundRuleResult> rules = findRules(round.roundId());

        return toResult(round, rules);
    }

    private InvestmentRoundInfo getActiveRound(Long userId) {
        return investmentRoundQueryPort.findActiveRoundByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_ACTIVE));
    }

    private List<GetActiveRoundRuleResult> findRules(Long roundId) {
        return investmentRuleQueryPort.findByRoundId(roundId).stream()
            .map(this::toRuleResult)
            .toList();
    }

    private GetActiveRoundRuleResult toRuleResult(InvestmentRuleInfo info) {
        return new GetActiveRoundRuleResult(info.ruleId(), info.ruleType(), info.thresholdValue());
    }

    private GetActiveRoundResult toResult(InvestmentRoundInfo round, List<GetActiveRoundRuleResult> rules) {
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
            rules
        );
    }
}
