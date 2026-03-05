package ksh.tryptobackend.ranking.application.port.out;

import java.math.BigDecimal;

public interface EmergencyFundingSnapshotPort {

    BigDecimal sumByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
