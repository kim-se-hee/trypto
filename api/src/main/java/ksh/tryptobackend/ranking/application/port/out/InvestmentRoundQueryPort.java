package ksh.tryptobackend.ranking.application.port.out;

import java.util.Optional;

public interface InvestmentRoundQueryPort {

    Optional<Long> findActiveRoundId(Long userId);
}
