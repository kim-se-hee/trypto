package ksh.tryptobackend.investmentround.application.port.out;

import ksh.tryptobackend.investmentround.domain.model.InvestmentRule;

import java.util.List;

public interface InvestmentRulePersistencePort {

    List<InvestmentRule> saveAll(List<InvestmentRule> rules);
}
