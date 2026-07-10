package ksh.tryptobackend.regretanalysis.adapter.out.acl;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindInvestmentRulesUseCase;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.InvestmentRuleResult;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.RoundInfoResult;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRound;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRoundStatus;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRule;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("regretanalysisAclInvestmentRoundQueryAdapter")
@RequiredArgsConstructor
public class AclInvestmentRoundQueryAdapter implements InvestmentRoundQueryPort {

    private final FindRoundInfoUseCase findRoundInfoUseCase;
    private final FindInvestmentRulesUseCase findInvestmentRulesUseCase;

    @Override
    public AnalysisRound getRound(Long roundId) {
        RoundInfoResult result =
                findRoundInfoUseCase
                        .findById(roundId)
                        .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_FOUND));
        return toAnalysisRound(result);
    }

    @Override
    public AnalysisRules findRules(Long roundId) {
        return new AnalysisRules(
                findInvestmentRulesUseCase.findByRoundId(roundId).stream()
                        .map(this::toAnalysisRule)
                        .toList());
    }

    private AnalysisRound toAnalysisRound(RoundInfoResult result) {
        return new AnalysisRound(
                result.roundId(),
                result.userId(),
                result.initialSeed(),
                AnalysisRoundStatus.valueOf(result.status()),
                result.startedAt(),
                result.endedAt());
    }

    private AnalysisRule toAnalysisRule(InvestmentRuleResult result) {
        return new AnalysisRule(result.ruleId(), result.ruleType(), result.thresholdValue());
    }
}
