package ksh.tryptobackend.investmentround.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import ksh.tryptobackend.investmentround.domain.model.EmergencyFunding;

public interface EmergencyFundingQueryPort {

    BigDecimal sumAmountByRoundId(Long roundId);

    BigDecimal sumAmountByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<EmergencyFunding> findAllByRoundId(Long roundId);
}
