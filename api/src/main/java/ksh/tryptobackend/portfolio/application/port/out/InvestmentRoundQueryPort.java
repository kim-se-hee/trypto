package ksh.tryptobackend.portfolio.application.port.out;

import ksh.tryptobackend.portfolio.domain.vo.ActiveRounds;

public interface InvestmentRoundQueryPort {

    ActiveRounds findActiveRounds();
}
