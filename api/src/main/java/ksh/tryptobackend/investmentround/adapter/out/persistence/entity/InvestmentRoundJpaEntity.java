package ksh.tryptobackend.investmentround.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "investment_round")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InvestmentRoundJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "round_id")
    private Long id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "round_number", nullable = false)
    private long roundNumber;

    @Column(name = "initial_seed", nullable = false, precision = 30, scale = 8)
    private BigDecimal initialSeed;

    @Column(name = "emergency_funding_limit", nullable = false, precision = 30, scale = 8)
    private BigDecimal emergencyFundingLimit;

    @Column(name = "emergency_charge_count", nullable = false)
    private int emergencyChargeCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private RoundStatus status;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "round_id")
    private List<InvestmentRuleJpaEntity> rules = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "round_id")
    private List<EmergencyFundingJpaEntity> fundings = new ArrayList<>();

    public static InvestmentRoundJpaEntity fromDomain(InvestmentRound round) {
        InvestmentRoundJpaEntity entity = new InvestmentRoundJpaEntity();
        entity.id = round.getId();
        entity.version = round.getVersion();
        entity.userId = round.getUserId();
        entity.roundNumber = round.getRoundNumber();
        entity.initialSeed = round.getInitialSeed();
        entity.emergencyFundingLimit = round.getEmergencyFundingLimit();
        entity.emergencyChargeCount = round.getEmergencyChargeCount();
        entity.status = round.getStatus();
        entity.startedAt = round.getStartedAt();
        entity.endedAt = round.getEndedAt();

        entity.rules = round.getRules().rules().stream()
                .map(InvestmentRuleJpaEntity::fromDomain)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        entity.fundings = round.getFundings().values().stream()
                .map(EmergencyFundingJpaEntity::fromDomain)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        return entity;
    }

    public void updateFrom(InvestmentRound round) {
        this.emergencyChargeCount = round.getEmergencyChargeCount();
        this.status = round.getStatus();
        this.endedAt = round.getEndedAt();
        round.getFundings().values().stream()
                .filter(funding -> funding.id() == null)
                .map(EmergencyFundingJpaEntity::fromDomain)
                .forEach(this.fundings::add);
    }

    public InvestmentRound toDomain() {
        return InvestmentRound.reconstitute(
                id,
                version,
                userId,
                roundNumber,
                initialSeed,
                emergencyFundingLimit,
                emergencyChargeCount,
                status,
                startedAt,
                endedAt,
                rules.stream().map(InvestmentRuleJpaEntity::toRoundDomain).toList(),
                fundings.stream().map(EmergencyFundingJpaEntity::toDomain).toList());
    }
}
