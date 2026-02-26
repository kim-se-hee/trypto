package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.trading.application.port.out.HoldingPersistencePort;
import ksh.tryptobackend.trading.domain.model.Holding;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MockHoldingAdapter implements HoldingPersistencePort {

    private final Map<String, Holding> holdings = new ConcurrentHashMap<>();

    @Override
    public Optional<Holding> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        return Optional.ofNullable(holdings.get(key(walletId, coinId)));
    }

    @Override
    public Holding save(Holding holding) {
        holdings.put(key(holding.getWalletId(), holding.getCoinId()), holding);
        return holding;
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
