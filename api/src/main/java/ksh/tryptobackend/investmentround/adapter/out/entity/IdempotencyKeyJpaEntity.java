package ksh.tryptobackend.investmentround.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.UUID;
import ksh.tryptobackend.common.domain.vo.IdempotencyResourceType;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "idempotency_key",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_idempotency_key",
                    columnNames = {"idempotency_key"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IdempotencyKeyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "idempotency_key_id")
    private Long id;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "idempotency_key", nullable = false, length = 36)
    private UUID idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "resource_type", nullable = false, length = 40)
    private IdempotencyResourceType resourceType;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static IdempotencyKeyJpaEntity preempt(
            UUID idempotencyKey, IdempotencyResourceType resourceType, LocalDateTime now) {
        IdempotencyKeyJpaEntity entity = new IdempotencyKeyJpaEntity();
        entity.idempotencyKey = idempotencyKey;
        entity.resourceType = resourceType;
        entity.createdAt = now;
        return entity;
    }

    public void assignResource(Long resourceId) {
        this.resourceId = resourceId;
    }
}
