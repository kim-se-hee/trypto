package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.marketdata.application.port.out.LivePriceQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.LivePrices;

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
    public LivePrices getCurrentPrices(Set<Long> exchangeCoinIds) {
        Map<Long, BigDecimal> priceMap = exchangeCoinIds.stream()
                .collect(Collectors.toMap(id -> id, id -> prices.getOrDefault(id, BigDecimal.ZERO)));
        return new LivePrices(priceMap);
    }

    public void setPrice(Long exchangeCoinId, BigDecimal price) {
        prices.put(exchangeCoinId, price);
    }

    public void clear() {
        prices.clear();
    }
}
