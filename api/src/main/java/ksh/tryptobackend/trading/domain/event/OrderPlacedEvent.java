package ksh.tryptobackend.trading.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.HoldingSnapshot;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import ksh.tryptobackend.trading.domain.vo.Side;

public final class OrderPlacedEvent {

    private final Order order;
    private final MarketInfo market;
    private final HoldingSnapshot holdingSnapshot;

    private OrderPlacedEvent(Order order, MarketInfo market, HoldingSnapshot holdingSnapshot) {
        this.order = order;
        this.market = market;
        this.holdingSnapshot = holdingSnapshot;
    }

    public static OrderPlacedEvent of(Order order, MarketInfo market, HoldingSnapshot holdingSnapshot) {
        return new OrderPlacedEvent(order, market, holdingSnapshot);
    }

    // 판정 근거는 주문 시점 스냅샷을 쓴다. 커밋 이후 보유 정보를 다시 읽으면 시장가는 이번 체결이 이미 반영돼 있다
    public boolean atLoss() {
        return holdingSnapshot.atLoss();
    }

    public int averagingDownCount() {
        return holdingSnapshot.averagingDownCount();
    }

    public Long orderId() {
        return order.getId();
    }

    public Long walletId() {
        return order.getWalletId();
    }

    public Long exchangeCoinId() {
        return order.getExchangeCoinId();
    }

    public Long coinId() {
        return market.tradingPair().tradedCoinId();
    }

    public Long baseCoinId() {
        return market.tradingPair().quoteCoinId();
    }

    public Side side() {
        return order.getSide();
    }

    public BigDecimal limitPrice() {
        return order.getLimitPrice() != null ? order.getLimitPrice().value() : null;
    }

    public BigDecimal quantity() {
        return order.getQuantity().value();
    }

    public BigDecimal lockAmount() {
        return lock().amount();
    }

    public Long lockedCoinId() {
        return lock().coinId();
    }

    public boolean awaitsMatching() {
        return order.awaitsMatching();
    }

    public BigDecimal currentPrice() {
        return market.currentPrice().value();
    }

    public LocalDateTime createdAt() {
        return order.getCreatedAt();
    }

    public boolean isBuy() {
        return order.getSide() == Side.BUY;
    }

    private BalanceChange.Lock lock() {
        return order.planReservation(market.tradingPair());
    }
}
