package ksh.tryptobackend.trading.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import ksh.tryptobackend.common.domain.model.AggregateRoot;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.application.port.in.dto.command.PlaceOrderCommand;
import ksh.tryptobackend.trading.domain.event.OrderCanceledEvent;
import ksh.tryptobackend.trading.domain.event.OrderFilledEvent;
import ksh.tryptobackend.trading.domain.event.OrderPlacedEvent;
import ksh.tryptobackend.trading.domain.vo.BalanceChange;
import ksh.tryptobackend.trading.domain.vo.Fill;
import ksh.tryptobackend.trading.domain.vo.InterpretedOrderInput;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import ksh.tryptobackend.trading.domain.vo.Money;
import ksh.tryptobackend.trading.domain.vo.OrderInput;
import ksh.tryptobackend.trading.domain.vo.OrderMode;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingPair;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
public class Order extends AggregateRoot {

    private final Long walletId;
    private final Long exchangeCoinId;
    private final OrderMode mode;
    private final Quantity quantity;
    private final Price limitPrice;
    private final BigDecimal feeRate;
    private final LocalDateTime createdAt;

    private Long id;
    private Fill fill;
    private OrderStatus status;

    public static Order create(PlaceOrderCommand cmd, MarketInfo marketInfo, LocalDateTime now) {
        OrderMode mode = OrderMode.of(cmd.orderType(), cmd.side());
        InterpretedOrderInput interpreted = mode.interpret(new OrderInput(cmd.volume(), cmd.price()), marketInfo);

        Order order = Order.builder()
                .walletId(cmd.walletId())
                .exchangeCoinId(cmd.exchangeCoinId())
                .mode(mode)
                .quantity(interpreted.quantity())
                .limitPrice(interpreted.limitPrice())
                .feeRate(marketInfo.exchangeInfo().feeRate())
                .status(OrderStatus.PENDING)
                .createdAt(now)
                .build();

        order.registerEvent(OrderPlacedEvent.of(order, marketInfo));
        if (order.isMarketOrder()) {
            order.fill(marketInfo.currentPrice(), marketInfo.tradingPair().quoteScale(), now);
            order.registerEvent(OrderFilledEvent.of(order, marketInfo));
        }
        return order;
    }

    public static Order reconstitute(
            Long id,
            Long walletId,
            Long exchangeCoinId,
            Side side,
            OrderType orderType,
            Quantity quantity,
            BigDecimal limitPrice,
            BigDecimal feeRate,
            BigDecimal filledPrice,
            BigDecimal feeAmount,
            OrderStatus status,
            LocalDateTime createdAt,
            LocalDateTime filledAt) {
        Price price = (limitPrice != null) ? Price.of(limitPrice) : null;
        Fill fill = (filledPrice != null) ? new Fill(Price.of(filledPrice), Money.of(feeAmount), filledAt) : null;
        return Order.builder()
                .id(id)
                .walletId(walletId)
                .exchangeCoinId(exchangeCoinId)
                .mode(OrderMode.of(orderType, side))
                .quantity(quantity)
                .limitPrice(price)
                .feeRate(feeRate)
                .fill(fill)
                .status(status)
                .createdAt(createdAt)
                .build();
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("이미 식별자가 부여된 주문입니다 id=" + this.id);
        }
        this.id = id;
    }

    public void fill(Price executionPrice, int quoteScale, LocalDateTime now) {
        if (!isPending()) {
            throw new CustomException(ErrorCode.ORDER_NOT_FILLABLE);
        }
        if (!mode.canFillAt(limitPrice, executionPrice)) {
            throw new CustomException(ErrorCode.INVALID_FILL_PRICE);
        }
        this.fill = Fill.settle(executionPrice, quantity, feeRate, quoteScale, now);
        this.status = OrderStatus.FILLED;
    }

    public List<BalanceChange> cancel(Long requesterWalletId, TradingPair pair) {
        if (!ownedBy(requesterWalletId)) {
            throw new CustomException(ErrorCode.ORDER_NOT_FOUND);
        }
        if (isCanceled()) {
            return List.of();
        }
        if (!isPending()) {
            throw new CustomException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        this.status = OrderStatus.CANCELED;
        registerEvent(OrderCanceledEvent.of(this));
        return planCancellationRefund(pair);
    }

    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    public boolean isMarketOrder() {
        return mode.orderType() == OrderType.MARKET;
    }

    public boolean awaitsMatching() {
        return isLimitOrder() && isPending();
    }

    public Side getSide() {
        return mode.side();
    }

    public OrderType getOrderType() {
        return mode.orderType();
    }

    public BalanceChange.Lock planReservation(TradingPair pair) {
        return mode.planReservation(quantity, limitPrice, feeRate, fill, pair);
    }

    public List<BalanceChange> planSettlementChanges(TradingPair pair) {
        return mode.planSettlementChanges(quantity, limitPrice, feeRate, fill, pair);
    }

    public Money getFilledAmount() {
        return fill != null ? fill.filledPrice().times(quantity) : null;
    }

    public Price getFilledPrice() {
        return fill != null ? fill.filledPrice() : null;
    }

    public Money getFeeAmount() {
        return fill != null ? fill.fee() : null;
    }

    public LocalDateTime getFilledAt() {
        return fill != null ? fill.filledAt() : null;
    }

    private boolean ownedBy(Long walletId) {
        return this.walletId.equals(walletId);
    }

    private boolean isCanceled() {
        return this.status == OrderStatus.CANCELED;
    }

    private List<BalanceChange> planCancellationRefund(TradingPair pair) {
        BalanceChange.Lock lock = planReservation(pair);
        return List.of(new BalanceChange.Unlock(lock.coinId(), lock.amount()));
    }

    private boolean isLimitOrder() {
        return mode.orderType() == OrderType.LIMIT;
    }
}
