package ksh.tryptobackend.trading.adapter.out.entity;

import jakarta.persistence.*;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "rule_violation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RuleViolationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_id")
    private Long id;

    @Column(name = "order_id", insertable = false, updatable = false)
    private Long orderId;

    @Column(name = "swap_id")
    private Long swapId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "violation_reason", nullable = false)
    private String violationReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static RuleViolationJpaEntity fromOrderViolation(Long orderId, RuleViolation violation) {
        RuleViolationJpaEntity entity = new RuleViolationJpaEntity();
        entity.orderId = orderId;
        entity.ruleId = violation.ruleId();
        entity.violationReason = violation.violationReason();
        entity.createdAt = violation.createdAt();
        return entity;
    }
}
