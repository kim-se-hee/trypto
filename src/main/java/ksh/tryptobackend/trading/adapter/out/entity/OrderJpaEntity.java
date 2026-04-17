package ksh.tryptobackend.trading.adapter.out.entity;

import jakarta.persistence.*;
import ksh.tryptobackend.trading.domain.model.Order;
import ksh.tryptobackend.trading.domain.vo.Fee;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.trading.domain.vo.OrderType;
import ksh.tryptobackend.trading.domain.vo.Quantity;
import ksh.tryptobackend.trading.domain.vo.Side;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 64)
    private String idempotencyKey;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "exchange_coin_id", nullable = false)
    private Long exchangeCoinId;

    @Column(name = "coin_id", nullable = false)
    private Long coinId;

    @Column(name = "base_coin_id", nullable = false)
    private Long baseCoinId;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false, length = 10)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", nullable = false, length = 4)
    private Side side;

    @Column(name = "order_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal amount;

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

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "order_id")
    private java.util.List<RuleViolationJpaEntity> violations = new java.util.ArrayList<>();

    public static OrderJpaEntity fromDomain(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.id = order.getId();
        entity.idempotencyKey = order.getIdempotencyKey();
        entity.userId = order.getUserId();
        entity.walletId = order.getWalletId();
        entity.exchangeCoinId = order.getExchangeCoinId();
        entity.coinId = order.getCoinId();
        entity.baseCoinId = order.getBaseCoinId();
        entity.orderType = order.getOrderType();
        entity.side = order.getSide();
        entity.amount = order.getAmount();
        entity.quantity = order.getQuantity().value();
        entity.price = order.getPrice();
        entity.filledPrice = order.getFilledPrice();
        entity.fee = order.getFee() != null ? order.getFee().amount() : null;
        entity.feeRate = order.getFee() != null ? order.getFee().rate() : null;
        entity.status = order.getStatus();
        entity.createdAt = order.getCreatedAt();
        entity.filledAt = order.getFilledAt();
        entity.violations = order.getViolations().stream()
            .map(v -> RuleViolationJpaEntity.fromOrderViolation(order.getId(), v))
            .toList();
        return entity;
    }

    public Order toDomain() {
        Fee domainFee = (fee != null && feeRate != null) ? Fee.of(fee, feeRate) : null;
        java.util.List<ksh.tryptobackend.trading.domain.model.RuleViolation> domainViolations =
            violations.stream()
                .map(v -> new ksh.tryptobackend.trading.domain.model.RuleViolation(
                    v.getRuleId(), v.getViolationReason(), v.getCreatedAt()))
                .toList();
        return Order.reconstitute(
            id, idempotencyKey, userId, walletId, exchangeCoinId, coinId, baseCoinId,
            side, orderType, amount, new Quantity(quantity),
            price, filledPrice, domainFee, status,
            createdAt, filledAt, domainViolations
        );
    }
}
