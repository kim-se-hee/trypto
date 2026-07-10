package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.domain.vo.ActiveRounds;

public interface InvestmentRoundQueryPort {

    Long getActiveRoundId(Long userId);

    ActiveRounds findActiveRounds();
}
