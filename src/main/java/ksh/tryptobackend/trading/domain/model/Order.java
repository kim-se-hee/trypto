package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder(access = lombok.AccessLevel.PRIVATE)
public class Order {

    private final Long id;
    private final UUID idempotencyKey;
    private final Long walletId;
    private final Long exchangeCoinId;
    private final Side side;
    private final OrderType orderType;
    private BigDecimal amount;
    private final Quantity quantity;
    private final BigDecimal price;
    private BigDecimal filledPrice;
    private Fee fee;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime filledAt;

    public static Order createMarketBuyOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                             BigDecimal amount, BigDecimal currentPrice, TradingVenue venue,
                                             LocalDateTime now) {
        venue.validateOrderAmount(amount);
        Quantity quantity = Quantity.fromAmountAndPrice(amount, currentPrice);
        BigDecimal filledAmount = quantity.value().multiply(currentPrice);
        Fee fee = venue.calculateFee(filledAmount);

        return createOrder(idempotencyKey, walletId, exchangeCoinId,
            Side.BUY, OrderType.MARKET, filledAmount, quantity, null, currentPrice, fee, now);
    }

    public static Order createMarketSellOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                              BigDecimal sellQuantity, BigDecimal currentPrice, TradingVenue venue,
                                              LocalDateTime now) {
        BigDecimal filledAmount = sellQuantity.multiply(currentPrice);
        Fee fee = venue.calculateFee(filledAmount);

        return createOrder(idempotencyKey, walletId, exchangeCoinId,
            Side.SELL, OrderType.MARKET, filledAmount, new Quantity(sellQuantity), null, currentPrice, fee, now);
    }

    public static Order createLimitBuyOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                            BigDecimal amount, BigDecimal limitPrice, TradingVenue venue,
                                            LocalDateTime now) {
        if (limitPrice == null) {
            throw new CustomException(ErrorCode.PRICE_REQUIRED_FOR_LIMIT);
        }
        venue.validateOrderAmount(amount);
        Quantity quantity = Quantity.fromAmountAndPrice(amount, limitPrice);
        BigDecimal filledAmount = quantity.value().multiply(limitPrice);
        Fee fee = venue.calculateFee(filledAmount);

        return createOrder(idempotencyKey, walletId, exchangeCoinId,
            Side.BUY, OrderType.LIMIT, filledAmount, quantity, limitPrice, limitPrice, fee, now);
    }

    public static Order createLimitSellOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                             BigDecimal sellQuantity, BigDecimal limitPrice, TradingVenue venue,
                                             LocalDateTime now) {
        if (limitPrice == null) {
            throw new CustomException(ErrorCode.PRICE_REQUIRED_FOR_LIMIT);
        }
        BigDecimal filledAmount = sellQuantity.multiply(limitPrice);
        Fee fee = venue.calculateFee(filledAmount);

        return createOrder(idempotencyKey, walletId, exchangeCoinId,
            Side.SELL, OrderType.LIMIT, filledAmount, new Quantity(sellQuantity), limitPrice, limitPrice, fee, now);
    }

    public static Order reconstitute(Long id, UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                     Side side, OrderType orderType, BigDecimal amount, Quantity quantity,
                                     BigDecimal price, BigDecimal filledPrice, Fee fee, OrderStatus status,
                                     LocalDateTime createdAt, LocalDateTime filledAt) {
        return Order.builder()
            .id(id)
            .idempotencyKey(idempotencyKey)
            .walletId(walletId)
            .exchangeCoinId(exchangeCoinId)
            .side(side)
            .orderType(orderType)
            .amount(amount)
            .quantity(quantity)
            .price(price)
            .filledPrice(filledPrice)
            .fee(fee)
            .status(status)
            .createdAt(createdAt)
            .filledAt(filledAt)
            .build();
    }


    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            return;
        }
        if (!isCancellable()) {
            throw new CustomException(ErrorCode.ORDER_NOT_CANCELLABLE);
        }
        this.status = OrderStatus.CANCELLED;
    }

    public boolean isMarketOrder() {
        return this.orderType == OrderType.MARKET;
    }

    public boolean isCancellable() {
        return this.status == OrderStatus.PENDING;
    }

    public boolean isAlreadyCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    public BigDecimal getFilledAmount() {
        return quantity.value().multiply(filledPrice != null ? filledPrice : price);
    }

    public BigDecimal getTotalCostForBuy() {
        return getFilledAmount().add(fee.amount());
    }

    private static Order createOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                     Side side, OrderType orderType, BigDecimal amount,
                                     Quantity quantity, BigDecimal price, BigDecimal filledPrice,
                                     Fee fee, LocalDateTime now) {
        return Order.builder()
            .idempotencyKey(idempotencyKey)
            .walletId(walletId)
            .exchangeCoinId(exchangeCoinId)
            .side(side)
            .orderType(orderType)
            .amount(amount)
            .quantity(quantity)
            .price(price)
            .filledPrice(filledPrice)
            .fee(fee)
            .status(orderType == OrderType.MARKET ? OrderStatus.FILLED : OrderStatus.PENDING)
            .createdAt(now)
            .filledAt(orderType == OrderType.MARKET ? now : null)
            .build();
    }
}
