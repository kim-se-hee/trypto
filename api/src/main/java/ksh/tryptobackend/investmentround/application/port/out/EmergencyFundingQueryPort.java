package ksh.tryptobackend.investmentround.application.port.out;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.investmentround.domain.model.EmergencyFunding;

public interface EmergencyFundingQueryPort {

    Optional<EmergencyFunding> findById(Long fundingId);

    BigDecimal sumAmountByRoundId(Long roundId);

    BigDecimal sumAmountByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<EmergencyFunding> findAllByRoundId(Long roundId);
}
