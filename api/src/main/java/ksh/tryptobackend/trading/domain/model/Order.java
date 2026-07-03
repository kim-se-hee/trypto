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
import ksh.tryptobackend.trading.domain.vo.ExchangeInfo;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.Fill;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import ksh.tryptobackend.trading.domain.vo.Money;
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

    private Long id;
    private final String idempotencyKey;
    private final Long walletId;
    private final Long exchangeCoinId;
    private final Side side;
    private final OrderType orderType;
    private final Quantity quantity;
    private final Price limitPrice;
    private final BigDecimal feeRate;
    private final LocalDateTime createdAt;

    private Fill fill;
    private OrderStatus status;

    public static Order create(PlaceOrderCommand cmd, MarketInfo marketInfo, LocalDateTime now) {
        ExchangeInfo exchange = marketInfo.exchangeInfo();
        Price referencePrice = resolveReferencePrice(cmd, marketInfo);
        Quantity quantity = resolveQuantity(cmd, referencePrice, exchange);
        Fill fill = resolveFill(cmd, quantity, referencePrice, exchange, now);

        Order order =
                Order.builder()
                        .idempotencyKey(cmd.idempotencyKey())
                        .walletId(cmd.walletId())
                        .exchangeCoinId(cmd.exchangeCoinId())
                        .side(cmd.side())
                        .orderType(cmd.orderType())
                        .quantity(quantity)
                        .limitPrice(cmd.orderType() == OrderType.LIMIT ? referencePrice : null)
                        .feeRate(exchange.feeRate())
                        .fill(fill)
                        .status(fill != null ? OrderStatus.FILLED : OrderStatus.PENDING)
                        .createdAt(now)
                        .build();

        order.registerEvent(OrderPlacedEvent.of(order, marketInfo));
        if (order.isFilled()) {
            order.registerEvent(OrderFilledEvent.of(order, marketInfo));
        }
        return order;
    }

    public void assignId(Long id) {
        if (this.id != null) {
            throw new IllegalStateException("이미 식별자가 부여된 주문입니다 id=" + this.id);
        }
        this.id = id;
    }

    public void fill(BigDecimal executionPrice, LocalDateTime now) {
        if (!isPending()) {
            throw new CustomException(ErrorCode.ORDER_NOT_FILLABLE);
        }
        Price price = Price.of(executionPrice);
        Money feeAmount = Fee.calculate(price.times(quantity), feeRate).amount();
        this.fill = new Fill(price, feeAmount, now);
        this.status = OrderStatus.FILLED;
    }

    public void cancel() {
        if (isAlreadyCancelled()) {
            return;
        }
        if (!isCancellable()) {
            throw new CustomException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        this.status = OrderStatus.CANCELLED;
        registerEvent(new OrderCanceledEvent(this));
    }

    public boolean isPending() {
        return this.status == OrderStatus.PENDING;
    }

    public boolean isFilled() {
        return this.status == OrderStatus.FILLED;
    }

    public boolean isMarketOrder() {
        return this.orderType == OrderType.MARKET;
    }

    public boolean isLimitOrder() {
        return this.orderType == OrderType.LIMIT;
    }

    public boolean isBuyOrder() {
        return this.side == Side.BUY;
    }

    public boolean awaitsMatching() {
        return isLimitOrder() && isPending();
    }

    public boolean isCancellable() {
        return this.status == OrderStatus.PENDING;
    }

    public boolean isAlreadyCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    public boolean isOwnedBy(Long walletId) {
        return this.walletId.equals(walletId);
    }

    public BigDecimal lockAmount() {
        if (!isBuyOrder()) return quantity.value();
        return isMarketOrder() ? getSettlementDebit().value() : getReservedDebit().value();
    }

    public Money getFilledAmount() {
        return fill != null ? fill.filledPrice().times(quantity) : null;
    }

    public Money getReservedDebit() {
        Money notional = limitPrice.times(quantity);
        return notional.plus(Fee.calculate(notional, feeRate).amount());
    }

    public Money getSettlementDebit() {
        return getFilledAmount().plus(fill.fee());
    }

    public Money getSettlementCredit() {
        return getFilledAmount().minus(fill.fee());
    }

    public BalanceChange planReservation(TradingPair pair) {
        return new BalanceChange.Lock(pair.lockedCoinId(side), lockAmount());
    }

    public List<BalanceChange> planSettlementChanges(TradingPair pair) {
        return OrderMode.of(orderType, side).planSettlementChanges(this, pair);
    }

    public BalanceChange planCancellationRefund(TradingPair pair) {
        return new BalanceChange.Unlock(pair.lockedCoinId(side), lockAmount());
    }

    public Price getFilledPrice() {
        return fill != null ? fill.filledPrice() : null;
    }

    public Fee getFee() {
        return fill != null ? Fee.of(fill.fee(), feeRate) : null;
    }

    public LocalDateTime getFilledAt() {
        return fill != null ? fill.filledAt() : null;
    }

    private static Price resolveReferencePrice(PlaceOrderCommand cmd, MarketInfo marketInfo) {
        if (cmd.orderType() == OrderType.MARKET) {
            return marketInfo.currentPrice();
        }
        if (cmd.price() == null) {
            throw new CustomException(ErrorCode.PRICE_REQUIRED_FOR_LIMIT);
        }
        return Price.of(cmd.price());
    }

    private static Quantity resolveQuantity(
            PlaceOrderCommand cmd, Price referencePrice, ExchangeInfo exchange) {
        if (cmd.side() == Side.SELL) {
            return Quantity.of(cmd.amount());
        }
        exchange.validateOrderAmount(cmd.amount());
        return Quantity.from(cmd.amount(), referencePrice);
    }

    private static Fill resolveFill(
            PlaceOrderCommand cmd,
            Quantity quantity,
            Price referencePrice,
            ExchangeInfo exchange,
            LocalDateTime now) {
        if (cmd.orderType() == OrderType.LIMIT) {
            return null;
        }
        Fee fee = exchange.calculateFee(referencePrice.times(quantity));
        return new Fill(referencePrice, fee.amount(), now);
    }

    public static Order reconstitute(
            Long id,
            String idempotencyKey,
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
        Fill fill =
                (filledPrice != null)
                        ? new Fill(Price.of(filledPrice), Money.of(feeAmount), filledAt)
                        : null;
        return Order.builder()
                .id(id)
                .idempotencyKey(idempotencyKey)
                .walletId(walletId)
                .exchangeCoinId(exchangeCoinId)
                .side(side)
                .orderType(orderType)
                .quantity(quantity)
                .limitPrice(price)
                .feeRate(feeRate)
                .fill(fill)
                .status(status)
                .createdAt(createdAt)
                .build();
    }
}
