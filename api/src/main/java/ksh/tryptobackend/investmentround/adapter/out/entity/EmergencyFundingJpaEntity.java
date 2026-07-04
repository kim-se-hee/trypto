package ksh.tryptobackend.investmentround.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import ksh.tryptobackend.investmentround.domain.model.EmergencyFunding;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "emergency_funding")
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

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static EmergencyFundingJpaEntity fromDomain(EmergencyFunding funding) {
        EmergencyFundingJpaEntity entity = new EmergencyFundingJpaEntity();
        entity.exchangeId = funding.exchangeId();
        entity.amount = funding.amount();
        entity.createdAt = funding.createdAt();
        return entity;
    }

    public EmergencyFunding toDomain() {
        return EmergencyFunding.reconstitute(id, exchangeId, amount, createdAt);
    }
}
