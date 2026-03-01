package ksh.tryptobackend.regretanalysis.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "violation_detail")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ViolationDetailJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_detail_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private RegretReportJpaEntity report;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "coin_id", nullable = false)
    private Long coinId;

    @Column(name = "loss_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal lossAmount;

    @Column(name = "profit_loss", nullable = false, precision = 30, scale = 8)
    private BigDecimal profitLoss;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    static ViolationDetailJpaEntity fromDomain(ViolationDetail detail, RegretReportJpaEntity report) {
        ViolationDetailJpaEntity entity = new ViolationDetailJpaEntity();
        entity.id = detail.getViolationDetailId();
        entity.report = report;
        entity.orderId = detail.getOrderId();
        entity.ruleId = detail.getRuleId();
        entity.coinId = detail.getCoinId();
        entity.lossAmount = detail.getLossAmount();
        entity.profitLoss = detail.getProfitLoss();
        entity.occurredAt = detail.getOccurredAt();
        return entity;
    }

    ViolationDetail toDomain() {
        return ViolationDetail.reconstitute(
            id, report.getId(), orderId, ruleId,
            coinId, lossAmount, profitLoss, occurredAt
        );
    }
}
