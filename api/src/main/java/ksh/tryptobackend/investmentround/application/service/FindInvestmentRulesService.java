package ksh.tryptobackend.investmentround.application.service;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.FindInvestmentRulesUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.InvestmentRuleResult;
import ksh.tryptobackend.investmentround.application.port.out.RuleSettingQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindInvestmentRulesService implements FindInvestmentRulesUseCase {

    private final RuleSettingQueryPort ruleSettingQueryPort;

    @Override
    public List<InvestmentRuleResult> findByRoundId(Long roundId) {
        return ruleSettingQueryPort.findByRoundId(roundId).stream()
                .map(InvestmentRuleResult::from)
                .toList();
    }
}
