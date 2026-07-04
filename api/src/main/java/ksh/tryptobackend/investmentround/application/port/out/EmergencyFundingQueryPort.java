package ksh.tryptobackend.investmentround.application.port.out;

import java.math.BigDecimal;

public interface EmergencyFundingQueryPort {

    BigDecimal sumAmountByRoundId(Long roundId);

    BigDecimal sumAmountByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
