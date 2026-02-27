package ksh.tryptobackend.investmentround.application.port.out;

import ksh.tryptobackend.investmentround.application.port.out.dto.InvestmentRuleInfo;

import java.util.List;

public interface InvestmentRuleQueryPort {

    List<InvestmentRuleInfo> findByRoundId(Long roundId);
}
