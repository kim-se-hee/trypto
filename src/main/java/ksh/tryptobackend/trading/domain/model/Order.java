package ksh.tryptobackend.trading.domain.model;

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

    public BigDecimal getFilledAmount() {
        if (side == Side.BUY) {
            return quantity.multiply(filledPrice != null ? filledPrice : price);
        }
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