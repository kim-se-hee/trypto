package ksh.tryptobackend.trading.domain.model;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderAmountPolicy;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
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
    private BigDecimal orderAmount;
    private final Quantity quantity;
    private final BigDecimal price;
    private BigDecimal filledPrice;
    private Fee fee;
    private OrderStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime filledAt;

    public static Order createMarketBuyOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                             BigDecimal orderAmount, BigDecimal currentPrice, BigDecimal feeRate,
                                             String baseCurrencySymbol, LocalDateTime now) {
        OrderAmountPolicy.of(baseCurrencySymbol).validate(orderAmount);
        Quantity quantity = Quantity.fromDivision(orderAmount, currentPrice);
        BigDecimal filledAmount = quantity.value().multiply(currentPrice);
        Fee fee = Fee.calculate(filledAmount, feeRate);

        return createOrder(idempotencyKey, walletId, exchangeCoinId,
            Side.BUY, OrderType.MARKET, filledAmount, quantity, null, currentPrice, fee, now);
    }

    public static Order createMarketSellOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                              BigDecimal sellQuantity, BigDecimal currentPrice, BigDecimal feeRate,
                                              LocalDateTime now) {
        BigDecimal filledAmount = sellQuantity.multiply(currentPrice);
        Fee fee = Fee.calculate(filledAmount, feeRate);

        return createOrder(idempotencyKey, walletId, exchangeCoinId,
            Side.SELL, OrderType.MARKET, filledAmount, new Quantity(sellQuantity), null, currentPrice, fee, now);
    }

    public static Order createLimitBuyOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                            BigDecimal orderAmount, BigDecimal limitPrice, BigDecimal feeRate,
                                            String baseCurrencySymbol, LocalDateTime now) {
        validateLimitPrice(limitPrice);
        OrderAmountPolicy.of(baseCurrencySymbol).validate(orderAmount);
        Quantity quantity = Quantity.fromDivision(orderAmount, limitPrice);
        BigDecimal filledAmount = quantity.value().multiply(limitPrice);
        Fee fee = Fee.calculate(filledAmount, feeRate);

        return createOrder(idempotencyKey, walletId, exchangeCoinId,
            Side.BUY, OrderType.LIMIT, filledAmount, quantity, limitPrice, null, fee, now);
    }

    public static Order createLimitSellOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                             BigDecimal sellQuantity, BigDecimal limitPrice, BigDecimal feeRate,
                                             LocalDateTime now) {
        validateLimitPrice(limitPrice);
        BigDecimal filledAmount = sellQuantity.multiply(limitPrice);
        Fee fee = Fee.calculate(filledAmount, feeRate);

        return createOrder(idempotencyKey, walletId, exchangeCoinId,
            Side.SELL, OrderType.LIMIT, filledAmount, new Quantity(sellQuantity), limitPrice, null, fee, now);
    }

    public static Order reconstitute(Long id, UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                     Side side, OrderType orderType, BigDecimal orderAmount, Quantity quantity,
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

    private static void validateLimitPrice(BigDecimal limitPrice) {
        if (limitPrice == null) {
            throw new CustomException(ErrorCode.PRICE_REQUIRED_FOR_LIMIT);
        }
    }

    private static Order createOrder(UUID idempotencyKey, Long walletId, Long exchangeCoinId,
                                     Side side, OrderType orderType, BigDecimal orderAmount,
                                     Quantity quantity, BigDecimal price, BigDecimal filledPrice,
                                     Fee fee, LocalDateTime now) {
        return Order.builder()
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
            .status(orderType == OrderType.MARKET ? OrderStatus.FILLED : OrderStatus.PENDING)
            .createdAt(now)
            .filledAt(orderType == OrderType.MARKET ? now : null)
            .build();
    }
}
