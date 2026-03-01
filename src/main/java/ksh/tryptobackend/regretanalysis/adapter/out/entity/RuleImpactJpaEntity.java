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
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.vo.ImpactGap;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "rule_impact")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RuleImpactJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_impact_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private RegretReportJpaEntity report;

    @Column(name = "rule_id", nullable = false)
    private Long ruleId;

    @Column(name = "violation_count", nullable = false)
    private int violationCount;

    @Column(name = "total_loss_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalLossAmount;

    @Column(name = "impact_gap", nullable = false, precision = 10, scale = 4)
    private BigDecimal impactGap;

    static RuleImpactJpaEntity fromDomain(RuleImpact ruleImpact, RegretReportJpaEntity report) {
        RuleImpactJpaEntity entity = new RuleImpactJpaEntity();
        entity.id = ruleImpact.getRuleImpactId();
        entity.report = report;
        entity.ruleId = ruleImpact.getRuleId();
        entity.violationCount = ruleImpact.getViolationCount();
        entity.totalLossAmount = ruleImpact.getTotalLossAmount();
        entity.impactGap = ruleImpact.getImpactGap().value();
        return entity;
    }

    RuleImpact toDomain() {
        return RuleImpact.reconstitute(
            id, report.getId(), ruleId,
            violationCount, totalLossAmount,
            ImpactGap.of(impactGap)
        );
    }
}
