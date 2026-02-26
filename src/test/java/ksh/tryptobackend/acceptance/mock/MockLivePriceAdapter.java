package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.trading.application.port.out.LivePricePort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MockLivePriceAdapter implements LivePricePort {

    private final Map<Long, BigDecimal> prices = new ConcurrentHashMap<>();

    @Override
    public BigDecimal getCurrentPrice(Long exchangeCoinId) {
        return prices.getOrDefault(exchangeCoinId, BigDecimal.ZERO);
    }

    public void setPrice(Long exchangeCoinId, BigDecimal price) {
        prices.put(exchangeCoinId, price);
    }

    public void clear() {
        prices.clear();
    }
}
