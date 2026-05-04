package ksh.tryptobackend.investmentround.application.port.out;

import java.util.Optional;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;

public interface InvestmentRoundCommandPort {

    boolean existsActiveRoundByUserId(Long userId);

    long countByUserId(Long userId);

    Optional<InvestmentRound> findById(Long roundId);

    InvestmentRound save(InvestmentRound round);
}
