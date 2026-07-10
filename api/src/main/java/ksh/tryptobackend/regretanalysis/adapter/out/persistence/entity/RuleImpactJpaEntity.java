package ksh.tryptobackend.regretanalysis.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.vo.ImpactGap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "rule_impact")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RuleImpactJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_impact_id")
    private Long id;

    @Column(name = "report_id", insertable = false, updatable = false)
    private Long reportId;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "violation_count", nullable = false)
    private int violationCount;

    @Column(name = "total_loss_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalLossAmount;

    @Column(name = "impact_gap", nullable = false, precision = 10, scale = 4)
    private BigDecimal impactGap;

    static RuleImpactJpaEntity fromDomain(RuleImpact ruleImpact) {
        RuleImpactJpaEntity entity = new RuleImpactJpaEntity();
        entity.id = ruleImpact.getRuleImpactId();
        entity.ruleId = ruleImpact.getRuleId();
        entity.violationCount = ruleImpact.getViolationCount();
        entity.totalLossAmount = ruleImpact.getTotalLossAmount();
        entity.impactGap = ruleImpact.getImpactGap().value();
        return entity;
    }

    RuleImpact toDomain() {
        return RuleImpact.reconstitute(
                id, reportId, ruleId, violationCount, totalLossAmount, ImpactGap.of(impactGap));
    }
}
