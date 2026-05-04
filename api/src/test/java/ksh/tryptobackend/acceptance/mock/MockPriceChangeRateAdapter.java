package ksh.tryptobackend.acceptance.mock;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import ksh.tryptobackend.marketdata.application.port.out.PriceChangeRateQueryPort;

public class MockPriceChangeRateAdapter implements PriceChangeRateQueryPort {

    private final Map<Long, BigDecimal> changeRates = new ConcurrentHashMap<>();

    @Override
    public BigDecimal getChangeRate(Long exchangeCoinId) {
        return changeRates.getOrDefault(exchangeCoinId, BigDecimal.ZERO);
    }

    public void setChangeRate(Long exchangeCoinId, BigDecimal rate) {
        changeRates.put(exchangeCoinId, rate);
    }

    public void clear() {
        changeRates.clear();
    }
}
