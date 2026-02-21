package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Order {

    private static final int QUANTITY_SCALE = 8;

    private Long id;
    private final UUID idempotencyKey;
    private final Long walletId;
    private final Long exchangeCoinId;
    private final Side side;
    private final OrderType orderType;
    private final BigDecimal orderAmount;
    private final BigDecimal quantity;
    private final BigDecimal price;
    private BigDecimal filledPrice;
    private Fee fee;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime filledAt;

    @Builder(access = lombok.AccessLevel.PRIVATE)
    private Order(Long id, UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                  Side side, OrderType orderType, BigDecimal orderAmount, BigDecimal quantity,
                  BigDecimal price, BigDecimal filledPrice, Fee fee, OrderStatus status,
                  LocalDateTime createdAt, LocalDateTime filledAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.walletId = walletId;
        this.exchangeCoinId = exchangeCoinId;
        this.side = side;
        this.orderType = orderType;
        this.orderAmount = orderAmount;
        this.quantity = quantity;
        this.price = price;
        this.filledPrice = filledPrice;
        this.fee = fee;
        this.status = status;
        this.createdAt = createdAt;
        this.filledAt = filledAt;
    }

    public static Order createMarketBuyOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                             BigDecimal orderAmount, BigDecimal currentPrice, BigDecimal feeRate,
                                             LocalDateTime now) {
        BigDecimal quantity = calculateQuantity(orderAmount, currentPrice);
        BigDecimal filledAmount = quantity.multiply(currentPrice);
        Fee fee = Fee.calculate(filledAmount, feeRate);

        return Order.builder()
                .idempotencyKey(idempotencyKey)
                .walletId(walletId)
                .exchangeCoinId(exchangeCoinId)
                .side(Side.BUY)
                .orderType(OrderType.MARKET)
                .orderAmount(filledAmount)
                .quantity(quantity)
                .filledPrice(currentPrice)
                .fee(fee)
                .status(OrderStatus.FILLED)
                .createdAt(now)
                .filledAt(now)
                .build();
    }

    public static Order createMarketSellOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                              BigDecimal sellQuantity, BigDecimal currentPrice, BigDecimal feeRate,
                                              LocalDateTime now) {
        BigDecimal filledAmount = sellQuantity.multiply(currentPrice);
        Fee fee = Fee.calculate(filledAmount, feeRate);

        return Order.builder()
                .idempotencyKey(idempotencyKey)
                .walletId(walletId)
                .exchangeCoinId(exchangeCoinId)
                .side(Side.SELL)
                .orderType(OrderType.MARKET)
                .orderAmount(sellQuantity)
                .quantity(sellQuantity)
                .filledPrice(currentPrice)
                .fee(fee)
                .status(OrderStatus.FILLED)
                .createdAt(now)
                .filledAt(now)
                .build();
    }

    public static Order createLimitBuyOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                            BigDecimal orderAmount, BigDecimal limitPrice, BigDecimal feeRate,
                                            LocalDateTime now) {
        BigDecimal quantity = calculateQuantity(orderAmount, limitPrice);
        BigDecimal filledAmount = quantity.multiply(limitPrice);
        Fee fee = Fee.calculate(filledAmount, feeRate);

        return Order.builder()
                .idempotencyKey(idempotencyKey)
                .walletId(walletId)
                .exchangeCoinId(exchangeCoinId)
                .side(Side.BUY)
                .orderType(OrderType.LIMIT)
                .orderAmount(filledAmount)
                .quantity(quantity)
                .price(limitPrice)
                .fee(fee)
                .status(OrderStatus.PENDING)
                .createdAt(now)
                .build();
    }

    public static Order createLimitSellOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                             BigDecimal sellQuantity, BigDecimal limitPrice, BigDecimal feeRate,
                                             LocalDateTime now) {
        Fee fee = Fee.calculate(sellQuantity.multiply(limitPrice), feeRate);

        return Order.builder()
                .idempotencyKey(idempotencyKey)
                .walletId(walletId)
                .exchangeCoinId(exchangeCoinId)
                .side(Side.SELL)
                .orderType(OrderType.LIMIT)
                .orderAmount(sellQuantity)
                .quantity(sellQuantity)
                .price(limitPrice)
                .fee(fee)
                .status(OrderStatus.PENDING)
                .createdAt(now)
                .build();
    }

    public static Order reconstitute(Long id, UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                     Side side, OrderType orderType, BigDecimal orderAmount, BigDecimal quantity,
                                     BigDecimal price, BigDecimal filledPrice, Fee fee, OrderStatus status,
                                     LocalDateTime createdAt, LocalDateTime filledAt) {
        return Order.builder()
                .id(id)
                .idempotencyKey(idempotencyKey)
                .walletId(walletId)
                .exchangeCoinId(exchangeCoinId)
                .side(side)
                .orderType(orderType)
                .orderAmount(orderAmount)
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

    public boolean isCancellable() {
        return this.status == OrderStatus.PENDING;
    }

    public boolean isAlreadyCancelled() {
        return this.status == OrderStatus.CANCELLED;
    }

    public BigDecimal getFilledAmount() {
        return quantity.multiply(filledPrice != null ? filledPrice : price);
    }

    public BigDecimal getTotalCostForBuy() {
        return getFilledAmount().add(fee.getAmount());
    }

    public static BigDecimal calculateQuantity(BigDecimal orderAmount, BigDecimal price) {
        return orderAmount.divide(price, QUANTITY_SCALE, RoundingMode.FLOOR);
    }

    public void assignId(Long id) {
        this.id = id;
    }
}