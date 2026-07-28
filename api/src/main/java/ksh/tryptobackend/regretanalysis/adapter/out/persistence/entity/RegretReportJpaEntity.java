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
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptobackend.regretanalysis.domain.model.RegretReport;
import ksh.tryptobackend.regretanalysis.domain.model.RuleImpact;
import ksh.tryptobackend.regretanalysis.domain.model.ViolationDetail;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "regret_report",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_regret_report_round_exchange",
                        columnNames = {"round_id", "exchange_id"}))
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

    @Column(name = "total_violation_loss", nullable = false, precision = 30, scale = 8)
    private BigDecimal totalViolationLoss;

    @Column(name = "actual_asset", nullable = false, precision = 30, scale = 8)
    private BigDecimal actualAsset;

    @Column(name = "analysis_start", nullable = false)
    private LocalDate analysisStart;

    @Column(name = "analysis_end", nullable = false)
    private LocalDate analysisEnd;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "report_id")
    private List<RuleImpactJpaEntity> ruleImpacts = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "report_id")
    private List<ViolationDetailJpaEntity> violationDetails = new ArrayList<>();

    public static RegretReportJpaEntity fromDomain(RegretReport report) {
        RegretReportJpaEntity entity = new RegretReportJpaEntity();
        entity.id = report.getReportId();
        entity.userId = report.getUserId();
        entity.roundId = report.getRoundId();
        entity.exchangeId = report.getExchangeId();
        entity.totalViolations = report.getTotalViolations();
        entity.totalViolationLoss = report.getTotalViolationLoss();
        entity.actualAsset = report.getActualAsset();
        entity.analysisStart = report.getAnalysisStart();
        entity.analysisEnd = report.getAnalysisEnd();
        entity.createdAt = report.getCreatedAt();

        entity.replaceChildren(report);

        return entity;
    }

    /**
     * 이미 저장된 리포트의 내용을 새 분석 결과로 갈아끼운다. 같은 (라운드, 거래소) 리포트는 1건만 존재해야 하므로 배치는 행을 새로 넣는 대신 이 메서드로 기존 행을
     * 갱신한다. 식별 정보(userId·roundId·exchangeId)와 최초 생성 시각(createdAt)은 유지한다.
     */
    public void updateFrom(RegretReport report) {
        this.totalViolations = report.getTotalViolations();
        this.totalViolationLoss = report.getTotalViolationLoss();
        this.actualAsset = report.getActualAsset();
        this.analysisStart = report.getAnalysisStart();
        this.analysisEnd = report.getAnalysisEnd();

        replaceChildren(report);
    }

    /** 자식 목록을 통째로 교체한다. 목록에서 빠진 기존 자식은 orphanRemoval 설정에 따라 함께 삭제된다. */
    private void replaceChildren(RegretReport report) {
        ruleImpacts.clear();
        if (report.getRuleImpacts() != null) {
            report.getRuleImpacts().forEach(ri -> ruleImpacts.add(RuleImpactJpaEntity.fromDomain(ri)));
        }

        violationDetails.clear();
        if (report.getViolationDetails() != null) {
            report.getViolationDetails()
                    .toList()
                    .forEach(vd -> violationDetails.add(ViolationDetailJpaEntity.fromDomain(vd)));
        }
    }

    public RegretReport toDomain() {
        List<RuleImpact> domainRuleImpacts =
                ruleImpacts.stream().map(RuleImpactJpaEntity::toDomain).toList();
        List<ViolationDetail> domainViolationDetails = violationDetails.stream()
                .map(ViolationDetailJpaEntity::toDomain)
                .toList();

        return RegretReport.reconstitute(
                id,
                userId,
                roundId,
                exchangeId,
                totalViolations,
                totalViolationLoss,
                actualAsset,
                analysisStart,
                analysisEnd,
                createdAt,
                domainRuleImpacts,
                domainViolationDetails);
    }
}
