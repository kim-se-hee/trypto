package ksh.tryptobackend.regretanalysis.adapter.out.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import ksh.tryptobackend.regretanalysis.domain.vo.RealizedLoss;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "violation_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ViolationDetailJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_detail_id")
    private Long id;

    @Column(name = "report_id", insertable = false, updatable = false)
    private Long reportId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "coin_id", nullable = false)
    private Long coinId;

    @Column(name = "loss_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal lossAmount;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "violation_detail_id")
    private List<ViolationRealizationJpaEntity> realizations = new ArrayList<>();

    static ViolationDetailJpaEntity fromDomain(ViolationDetail detail) {
        ViolationDetailJpaEntity entity = new ViolationDetailJpaEntity();
        entity.id = detail.getViolationDetailId();
        entity.orderId = detail.getOrderId();
        entity.ruleId = detail.getRuleId();
        entity.coinId = detail.getCoinId();
        entity.lossAmount = detail.getLossAmount();
        entity.occurredAt = detail.getOccurredAt();
        detail.getRealizedLosses()
                .forEach(realized -> entity.realizations.add(ViolationRealizationJpaEntity.fromDomain(realized)));
        return entity;
    }

    public ViolationDetail toDomain() {
        List<RealizedLoss> realizedLosses = realizations.stream()
                .map(ViolationRealizationJpaEntity::toDomain)
                .toList();
        return ViolationDetail.reconstitute(
                id, reportId, orderId, ruleId, coinId, lossAmount, realizedLosses, occurredAt);
    }
}
