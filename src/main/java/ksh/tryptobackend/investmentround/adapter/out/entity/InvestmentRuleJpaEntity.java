package ksh.tryptobackend.investmentround.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ksh.tryptobackend.common.domain.vo.RuleType;
import ksh.tryptobackend.investmentround.domain.model.RuleSetting;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "investment_rule")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentRuleJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    @Column(name = "round_id", insertable = false, updatable = false)
    private Long roundId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 40)
    private RuleType ruleType;

    @Column(name = "threshold_value", nullable = false, precision = 30, scale = 8)
    private BigDecimal thresholdValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static InvestmentRuleJpaEntity fromDomain(RuleSetting rule) {
        InvestmentRuleJpaEntity entity = new InvestmentRuleJpaEntity();
        entity.id = rule.getRuleId();
        entity.roundId = rule.getRoundId();
        entity.ruleType = rule.getRuleType();
        entity.thresholdValue = rule.getThresholdValue();
        entity.createdAt = rule.getCreatedAt();
        return entity;
    }

    public RuleSetting toRoundDomain() {
        return RuleSetting.builder()
            .ruleId(id)
            .roundId(roundId)
            .ruleType(ruleType)
            .thresholdValue(thresholdValue)
            .createdAt(createdAt)
            .build();
    }
}
