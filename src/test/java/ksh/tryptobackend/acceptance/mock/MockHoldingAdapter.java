package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.portfolio.domain.model.Holding;
import ksh.tryptobackend.trading.application.port.out.HoldingPort;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MockHoldingAdapter implements HoldingPort {

    private final Map<String, Holding> holdings = new ConcurrentHashMap<>();

    @Override
    public Optional<HoldingData> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        Holding h = holdings.get(key(walletId, coinId));
        if (h == null) {
            return Optional.empty();
        }
        return Optional.of(new HoldingData(h.getAvgBuyPrice(), h.getTotalQuantity(), h.getAveragingDownCount()));
    }

    @Override
    public void applyBuy(Long walletId, Long coinId, BigDecimal filledPrice,
                         BigDecimal filledQuantity, BigDecimal currentPrice) {
        Holding h = holdings.computeIfAbsent(key(walletId, coinId),
            k -> Holding.empty(walletId, coinId));
        h.applyBuy(filledPrice, filledQuantity, currentPrice);
    }

    @Override
    public void applySell(Long walletId, Long coinId, BigDecimal filledQuantity) {
        Holding h = holdings.get(key(walletId, coinId));
        if (h != null) {
            h.applySell(filledQuantity);
        }
    }

    public void setHolding(Long walletId, Long coinId, BigDecimal avgBuyPrice,
                           BigDecimal totalQuantity, int averagingDownCount) {
        holdings.put(key(walletId, coinId), Holding.builder()
            .walletId(walletId)
            .coinId(coinId)
            .avgBuyPrice(avgBuyPrice)
            .totalQuantity(totalQuantity)
            .totalBuyAmount(avgBuyPrice.multiply(totalQuantity))
            .averagingDownCount(averagingDownCount)
            .build());
    }

    public void clear() {
        holdings.clear();
    }

    private String key(Long walletId, Long coinId) {
        return walletId + ":" + coinId;
    }
}
