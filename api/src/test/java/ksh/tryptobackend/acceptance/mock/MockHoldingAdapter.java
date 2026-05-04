package ksh.tryptobackend.acceptance.mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import ksh.tryptobackend.trading.application.port.out.HoldingCommandPort;
import ksh.tryptobackend.trading.application.port.out.HoldingQueryPort;
import ksh.tryptobackend.trading.domain.model.Holding;

public class MockHoldingAdapter implements HoldingCommandPort, HoldingQueryPort {

    private final Map<String, Holding> holdings = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public Optional<Holding> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        String k = key(walletId, coinId);
        locks.computeIfAbsent(k, ignore -> new ReentrantLock()).lock();
        return Optional.ofNullable(holdings.get(k));
    }

    @Override
    public List<Holding> findAllByWalletId(Long walletId) {
        return holdings.values().stream().filter(h -> h.getWalletId().equals(walletId)).toList();
    }

    @Override
    public Holding save(Holding holding) {
        String k = key(holding.getWalletId(), holding.getCoinId());
        holdings.put(k, holding);
        ReentrantLock lock = locks.get(k);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
        return holding;
    }

    public void setHolding(
            Long walletId,
            Long coinId,
            BigDecimal avgBuyPrice,
            BigDecimal totalQuantity,
            int averagingDownCount) {
        holdings.put(
                key(walletId, coinId),
                Holding.builder()
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
        locks.clear();
    }

    private String key(Long walletId, Long coinId) {
        return walletId + ":" + coinId;
    }
}
