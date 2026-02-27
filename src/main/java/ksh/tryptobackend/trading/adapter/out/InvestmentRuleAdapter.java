package ksh.tryptobackend.trading.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.InvestmentRuleQueryPort;
import ksh.tryptobackend.trading.application.port.out.InvestmentRulePort;
import ksh.tryptobackend.trading.domain.model.InvestmentRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InvestmentRuleAdapter implements InvestmentRulePort {

    private final InvestmentRuleQueryPort investmentRuleQueryPort;

    @Override
    public List<InvestmentRule> findByRoundId(Long roundId) {
        return investmentRuleQueryPort.findByRoundId(roundId).stream()
            .map(info -> InvestmentRule.of(info.ruleId(), info.ruleType(), info.thresholdValue()))
            .toList();
    }
}
