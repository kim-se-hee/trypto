package ksh.tryptobackend.investmentround.application.service;

import ksh.tryptobackend.investmentround.application.port.in.FindInvestmentRulesUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.InvestmentRuleResult;
import ksh.tryptobackend.investmentround.application.port.out.RuleSettingQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
