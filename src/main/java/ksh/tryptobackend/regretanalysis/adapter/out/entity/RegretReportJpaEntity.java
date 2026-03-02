package ksh.tryptobackend.regretanalysis.adapter.out.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "regret_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RegretReportJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "exchange_id", nullable = false)
    private Long exchangeId;

    @Column(name = "total_violations", nullable = false)
    private int totalViolations;

    @Column(name = "missed_profit", nullable = false, precision = 30, scale = 8)
    private BigDecimal missedProfit;

    @Column(name = "actual_profit_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal actualProfitRate;

    @Column(name = "rule_followed_profit_rate", nullable = false, precision = 10, scale = 4)
    private BigDecimal ruleFollowedProfitRate;

    @Column(name = "analysis_start", nullable = false)
    private LocalDate analysisStart;

    @Column(name = "analysis_end", nullable = false)
    private LocalDate analysisEnd;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RuleImpactJpaEntity> ruleImpacts = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ViolationDetailJpaEntity> violationDetails = new ArrayList<>();

    public static RegretReportJpaEntity fromDomain(RegretReport report) {
        RegretReportJpaEntity entity = new RegretReportJpaEntity();
        entity.id = report.getReportId();
        entity.userId = report.getUserId();
        entity.roundId = report.getRoundId();
        entity.exchangeId = report.getExchangeId();
        entity.totalViolations = report.getTotalViolations();
        entity.missedProfit = report.getMissedProfit();
        entity.actualProfitRate = report.getActualProfitRate();
        entity.ruleFollowedProfitRate = report.getRuleFollowedProfitRate();
        entity.analysisStart = report.getAnalysisStart();
        entity.analysisEnd = report.getAnalysisEnd();
        entity.createdAt = report.getCreatedAt();

        if (report.getRuleImpacts() != null) {
            report.getRuleImpacts().forEach(ri ->
                entity.ruleImpacts.add(RuleImpactJpaEntity.fromDomain(ri, entity)));
        }
        if (report.getViolationDetails() != null) {
            report.getViolationDetails().toList().forEach(vd ->
                entity.violationDetails.add(ViolationDetailJpaEntity.fromDomain(vd, entity)));
        }

        return entity;
    }

    public RegretReport toDomain() {
        List<RuleImpact> domainRuleImpacts = ruleImpacts.stream()
            .map(RuleImpactJpaEntity::toDomain)
            .toList();
        List<ViolationDetail> domainViolationDetails = violationDetails.stream()
            .map(ViolationDetailJpaEntity::toDomain)
            .toList();

        return RegretReport.reconstitute(
            id, userId, roundId, exchangeId,
            totalViolations, missedProfit,
            actualProfitRate, ruleFollowedProfitRate,
            analysisStart, analysisEnd, createdAt,
            domainRuleImpacts, domainViolationDetails
        );
    }
}
