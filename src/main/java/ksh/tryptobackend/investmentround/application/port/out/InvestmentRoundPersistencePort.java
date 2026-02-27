package ksh.tryptobackend.investmentround.application.port.out;

import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;

public interface InvestmentRoundPersistencePort {

    boolean existsActiveRoundByUserId(Long userId);

    long countByUserId(Long userId);

    InvestmentRound save(InvestmentRound round);
}
