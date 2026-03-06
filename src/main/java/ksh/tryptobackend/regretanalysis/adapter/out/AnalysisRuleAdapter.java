package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.investmentround.application.port.in.FindInvestmentRulesUseCase;
import ksh.tryptobackend.regretanalysis.application.port.out.AnalysisRulePort;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRule;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisRules;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnalysisRuleAdapter implements AnalysisRulePort {

    private final FindInvestmentRulesUseCase findInvestmentRulesUseCase;

    @Override
    public AnalysisRules findByRoundId(Long roundId) {
        return new AnalysisRules(
            findInvestmentRulesUseCase.findByRoundId(roundId).stream()
                .map(result -> new AnalysisRule(result.ruleId(), result.ruleType(), result.thresholdValue()))
                .toList()
        );
    }
}
