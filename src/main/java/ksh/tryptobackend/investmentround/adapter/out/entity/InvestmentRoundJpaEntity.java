package ksh.tryptobackend.investmentround.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import ksh.tryptobackend.investmentround.domain.model.InvestmentRound;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    public static InvestmentRoundJpaEntity fromDomain(InvestmentRound round) {
        InvestmentRoundJpaEntity entity = new InvestmentRoundJpaEntity();
        entity.id = round.getRoundId();
        entity.version = round.getVersion();
        entity.userId = round.getUserId();
        entity.roundNumber = round.getRoundNumber();
        entity.initialSeed = round.getInitialSeed();
        entity.emergencyFundingLimit = round.getEmergencyFundingLimit();
        entity.emergencyChargeCount = round.getEmergencyChargeCount();
        entity.status = round.getStatus();
        entity.startedAt = round.getStartedAt();
        entity.endedAt = round.getEndedAt();
        return entity;
    }

    public InvestmentRound toDomain() {
        return InvestmentRound.builder()
            .roundId(id)
            .version(version)
            .userId(userId)
            .roundNumber(roundNumber)
            .initialSeed(initialSeed)
            .emergencyFundingLimit(emergencyFundingLimit)
            .emergencyChargeCount(emergencyChargeCount)
            .status(status)
            .startedAt(startedAt)
            .endedAt(endedAt)
            .build();
    }
}
