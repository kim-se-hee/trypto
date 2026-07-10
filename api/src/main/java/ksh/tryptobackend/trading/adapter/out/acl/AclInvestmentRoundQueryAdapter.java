package ksh.tryptobackend.trading.adapter.out.acl;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.FindInvestmentRulesUseCase;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.InvestmentRuleResult;
import ksh.tryptobackend.trading.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.trading.domain.vo.InvestmentRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("tradingAclInvestmentRoundQueryAdapter")
@RequiredArgsConstructor
public class AclInvestmentRoundQueryAdapter implements InvestmentRoundQueryPort {

    private final FindInvestmentRulesUseCase findInvestmentRulesUseCase;

    @Override
    public List<InvestmentRule> findRulesByRoundId(Long roundId) {
        return findInvestmentRulesUseCase.findByRoundId(roundId).stream()
                .map(this::toInvestmentRule)
                .toList();
    }

    private InvestmentRule toInvestmentRule(InvestmentRuleResult result) {
        return new InvestmentRule(result.ruleId(), result.ruleType());
    }
}
