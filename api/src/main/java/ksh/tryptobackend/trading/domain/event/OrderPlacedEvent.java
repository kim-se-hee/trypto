package ksh.tryptobackend.trading.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import ksh.tryptobackend.trading.domain.vo.Side;

public final class OrderPlacedEvent {

    private final Order order;
    private final MarketInfo market;

    private OrderPlacedEvent(Order order, MarketInfo market) {
        this.order = order;
        this.market = market;
    }

    public static OrderPlacedEvent of(Order order, MarketInfo market) {
        return new OrderPlacedEvent(order, market);
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
