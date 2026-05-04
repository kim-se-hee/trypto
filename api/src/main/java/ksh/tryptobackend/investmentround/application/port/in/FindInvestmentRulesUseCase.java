package ksh.tryptobackend.investmentround.application.port.in;

import java.util.List;
import ksh.tryptobackend.investmentround.application.port.in.dto.result.InvestmentRuleResult;

public interface FindInvestmentRulesUseCase {

    List<InvestmentRuleResult> findByRoundId(Long roundId);
}
