package ksh.tryptobackend.portfolio.application.port.out;

import java.math.BigDecimal;
import ksh.tryptobackend.portfolio.domain.vo.ActiveRounds;

public interface InvestmentRoundQueryPort {

    ActiveRounds findActiveRounds();

    BigDecimal sumEmergencyFunding(Long roundId, Long exchangeId);
}
