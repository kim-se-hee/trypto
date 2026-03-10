package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.marketdata.application.port.out.LivePriceQueryPort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class MockLivePriceAdapter implements LivePriceQueryPort {

    private final Map<Long, BigDecimal> prices = new ConcurrentHashMap<>();

    @Override
    public BigDecimal getCurrentPrice(Long exchangeCoinId) {
        return prices.getOrDefault(exchangeCoinId, BigDecimal.ZERO);
    }

    @Override
    public Map<Long, BigDecimal> getCurrentPrices(Set<Long> exchangeCoinIds) {
        return exchangeCoinIds.stream()
                .collect(Collectors.toMap(id -> id, id -> prices.getOrDefault(id, BigDecimal.ZERO)));
    }

    public void setPrice(Long exchangeCoinId, BigDecimal price) {
        prices.put(exchangeCoinId, price);
    }

    public void clear() {
        prices.clear();
    }
}
