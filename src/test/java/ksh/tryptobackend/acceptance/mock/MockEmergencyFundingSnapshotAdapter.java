package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.ranking.application.port.out.EmergencyFundingSnapshotPort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockEmergencyFundingSnapshotAdapter implements EmergencyFundingSnapshotPort {

    private final Map<String, BigDecimal> fundings = new ConcurrentHashMap<>();

    @Override
    public BigDecimal sumByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return fundings.getOrDefault(key(roundId, exchangeId), BigDecimal.ZERO);
    }

    public void setFunding(Long roundId, Long exchangeId, BigDecimal amount) {
        fundings.put(key(roundId, exchangeId), amount);
    }

    public void clear() {
        fundings.clear();
    }

    private String key(Long roundId, Long exchangeId) {
        return roundId + ":" + exchangeId;
    }
}
