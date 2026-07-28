package ksh.tryptobackend.regretanalysis.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import ksh.tryptobackend.regretanalysis.domain.vo.RealizedLoss;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "violation_realization")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ViolationRealizationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "violation_realization_id")
    private Long id;

    @Column(name = "violation_detail_id", insertable = false, updatable = false)
    private Long violationDetailId;

    @Column(name = "realized_on", nullable = false)
    private LocalDate realizedOn;

    @Column(name = "loss_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal lossAmount;

    static ViolationRealizationJpaEntity fromDomain(RealizedLoss realizedLoss) {
        ViolationRealizationJpaEntity entity = new ViolationRealizationJpaEntity();
        entity.realizedOn = realizedLoss.realizedOn();
        entity.lossAmount = realizedLoss.amount();
        return entity;
    }

    RealizedLoss toDomain() {
        return new RealizedLoss(realizedOn, lossAmount);
    }
}
