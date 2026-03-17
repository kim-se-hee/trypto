package ksh.tryptobackend.trading.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ksh.tryptobackend.trading.domain.model.OrderFillFailure;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_fill_failure")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderFillFailureJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_fill_failure_id")
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(name = "attempted_price", nullable = false, precision = 30, scale = 8)
    private BigDecimal attemptedPrice;

    @Column(name = "failed_at", nullable = false)
    private LocalDateTime failedAt;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "resolved", nullable = false)
    private boolean resolved;

    public static OrderFillFailureJpaEntity fromDomain(OrderFillFailure failure) {
        OrderFillFailureJpaEntity entity = new OrderFillFailureJpaEntity();
        entity.id = failure.getId();
        entity.orderId = failure.getOrderId();
        entity.attemptedPrice = failure.getAttemptedPrice();
        entity.failedAt = failure.getFailedAt();
        entity.reason = failure.getReason();
        entity.resolved = failure.isResolved();
        return entity;
    }

    public OrderFillFailure toDomain() {
        return OrderFillFailure.builder()
            .id(id)
            .orderId(orderId)
            .attemptedPrice(attemptedPrice)
            .failedAt(failedAt)
            .reason(reason)
            .resolved(resolved)
            .build();
    }
}
