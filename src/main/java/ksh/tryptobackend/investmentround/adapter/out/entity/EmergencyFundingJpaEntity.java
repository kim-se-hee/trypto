package ksh.tryptobackend.investmentround.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import ksh.tryptobackend.investmentround.domain.model.EmergencyFunding;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "emergency_funding",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_emergency_funding_round_idempotency", columnNames = {"round_id", "idempotency_key"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmergencyFundingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "funding_id")
    private Long id;

    @Column(name = "round_id", insertable = false, updatable = false)
    private Long roundId;

    @Column(name = "exchange_id", nullable = false)
    private Long exchangeId;

    @Column(name = "amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal amount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "idempotency_key", nullable = false, length = 36)
    private UUID idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static EmergencyFundingJpaEntity fromDomain(EmergencyFunding funding) {
        EmergencyFundingJpaEntity entity = new EmergencyFundingJpaEntity();
        entity.id = funding.getFundingId();
        entity.roundId = funding.getRoundId();
        entity.exchangeId = funding.getExchangeId();
        entity.amount = funding.getAmount();
        entity.idempotencyKey = funding.getIdempotencyKey();
        entity.createdAt = funding.getCreatedAt();
        return entity;
    }

    public EmergencyFunding toDomain() {
        return EmergencyFunding.builder()
            .fundingId(id)
            .roundId(roundId)
            .exchangeId(exchangeId)
            .amount(amount)
            .idempotencyKey(idempotencyKey)
            .createdAt(createdAt)
            .build();
    }
}
