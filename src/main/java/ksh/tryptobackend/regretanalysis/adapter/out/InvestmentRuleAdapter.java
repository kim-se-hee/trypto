package ksh.tryptobackend.regretanalysis.adapter.out;

import ksh.tryptobackend.investmentround.application.port.out.InvestmentRuleQueryPort;
import ksh.tryptobackend.regretanalysis.application.port.out.InvestmentRulePort;
import ksh.tryptobackend.regretanalysis.application.port.out.dto.RuleInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("regretInvestmentRuleAdapter")
@RequiredArgsConstructor
public class InvestmentRuleAdapter implements InvestmentRulePort {

    private final InvestmentRuleQueryPort investmentRuleQueryPort;

    @Override
    public List<RuleInfo> findByRoundId(Long roundId) {
        return investmentRuleQueryPort.findByRoundId(roundId).stream()
            .map(info -> new RuleInfo(info.ruleId(), info.ruleType(), info.thresholdValue()))
            .toList();
    }
}
