package ksh.tryptobackend.trading.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "exchange_coin_id", nullable = false)
    private Long exchangeCoinId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 4)
    private Side side;

    @Column(name = "quantity", nullable = false, precision = 30, scale = 8)
    private BigDecimal quantity;

    @Column(name = "price", precision = 30, scale = 8)
    private BigDecimal price;

    @Column(name = "filled_price", precision = 30, scale = 8)
    private BigDecimal filledPrice;

    @Column(name = "fee", precision = 30, scale = 8)
    private BigDecimal fee;

    @Column(name = "fee_rate", precision = 10, scale = 8)
    private BigDecimal feeRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "filled_at")
    private LocalDateTime filledAt;

    public static OrderJpaEntity fromDomain(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.id = order.getId();
        entity.walletId = order.getWalletId();
        entity.exchangeCoinId = order.getExchangeCoinId();
        entity.orderType = order.getOrderType();
        entity.side = order.getSide();
        entity.quantity = order.getQuantity().value();
        entity.price = order.getLimitPrice() != null ? order.getLimitPrice().value() : null;
        entity.filledPrice = order.getFilledPrice() != null ? order.getFilledPrice().value() : null;
        entity.fee = order.getFeeAmount() != null ? order.getFeeAmount().value() : null;
        entity.feeRate = order.getFeeRate();
        entity.status = order.getStatus();
        entity.createdAt = order.getCreatedAt();
        entity.filledAt = order.getFilledAt();
        return entity;
    }

    public Order toDomain() {
        return Order.reconstitute(
                id,
                walletId,
                exchangeCoinId,
                side,
                orderType,
                Quantity.of(quantity),
                price,
                feeRate,
                filledPrice,
                fee,
                status,
                createdAt,
                filledAt);
    }
}
