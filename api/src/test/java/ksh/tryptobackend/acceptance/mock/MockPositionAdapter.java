package ksh.tryptobackend.acceptance.mock;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import ksh.tryptobackend.trading.application.port.out.PositionCommandPort;
import ksh.tryptobackend.trading.application.port.out.PositionQueryPort;
import ksh.tryptobackend.trading.domain.model.Position;
import ksh.tryptobackend.trading.domain.vo.Holding;
import ksh.tryptobackend.trading.domain.vo.Money;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.Quantity;

public class MockPositionAdapter implements PositionCommandPort, PositionQueryPort {

    private final Map<String, Position> positions = new ConcurrentHashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    @Override
    public Optional<Position> findByWalletIdAndCoinId(Long walletId, Long coinId) {
        return Optional.ofNullable(positions.get(key(walletId, coinId)));
    }

    // 잠금은 getOrCreate → save 읽기-수정-쓰기 구간만 보호한다. save 를 호출하지 않는
    // 조회 전용 경로(위반 판정 등)가 잠그면 해제할 곳이 없어 다음 주문이 영원히 대기한다.
    @Override
    public Position getOrCreate(Long walletId, Long coinId) {
        String k = key(walletId, coinId);
        locks.computeIfAbsent(k, ignore -> new ReentrantLock()).lock();
        return Optional.ofNullable(positions.get(k)).orElseGet(() -> Position.empty(walletId, coinId));
    }

    @Override
    public List<Position> findAllByWalletId(Long walletId) {
        return positions.values().stream()
                .filter(p -> p.getWalletId().equals(walletId))
                .toList();
    }

    @Override
    public Position save(Position position) {
        String k = key(position.getWalletId(), position.getCoinId());
        positions.put(k, position);
        ReentrantLock lock = locks.get(k);
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
        return position;
    }

    public void setHolding(
            Long walletId, Long coinId, BigDecimal avgBuyPrice, BigDecimal totalQuantity, int averagingDownCount) {
        positions.put(
                key(walletId, coinId),
                Position.builder()
                        .walletId(walletId)
                        .coinId(coinId)
                        .holding(new Holding(
                                Price.of(avgBuyPrice),
                                Quantity.of(totalQuantity),
                                Money.of(avgBuyPrice.multiply(totalQuantity))))
                        .averagingDownCount(averagingDownCount)
                        .build());
    }

    public void clear() {
        positions.clear();
        locks.clear();
    }

    private String key(Long walletId, Long coinId) {
        return walletId + ":" + coinId;
    }
}
