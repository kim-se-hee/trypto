package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.model.InvestmentRule;

import java.util.List;

public interface InvestmentRulePort {

    List<InvestmentRule> findByRoundId(Long roundId);
}
