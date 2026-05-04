package ksh.tryptobackend.investmentround.application.port.out;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import ksh.tryptobackend.investmentround.domain.model.EmergencyFunding;

public interface EmergencyFundingQueryPort {

    BigDecimal sumAmountByRoundId(Long roundId);

    BigDecimal sumAmountByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    Optional<EmergencyFunding> findByRoundIdAndIdempotencyKey(Long roundId, UUID idempotencyKey);
}
