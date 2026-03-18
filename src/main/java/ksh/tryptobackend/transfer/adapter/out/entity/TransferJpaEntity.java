package ksh.tryptobackend.transfer.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import ksh.tryptobackend.transfer.domain.model.Transfer;
import ksh.tryptobackend.transfer.domain.vo.TransferStatus;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "transfer")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TransferJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transfer_id")
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, columnDefinition = "BINARY(16)")
    private UUID idempotencyKey;

    @Column(name = "from_wallet_id", nullable = false)
    private Long fromWalletId;

    @Column(name = "to_wallet_id", nullable = false)
    private Long toWalletId;

    @Column(name = "coin_id", nullable = false)
    private Long coinId;

    @Column(name = "amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private TransferStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static TransferJpaEntity fromDomain(Transfer transfer) {
        TransferJpaEntity entity = new TransferJpaEntity();
        entity.id = transfer.getTransferId();
        entity.idempotencyKey = transfer.getIdempotencyKey();
        entity.fromWalletId = transfer.getFromWalletId();
        entity.toWalletId = transfer.getToWalletId();
        entity.coinId = transfer.getCoinId();
        entity.amount = transfer.getAmount();
        entity.status = transfer.getStatus();
        entity.createdAt = transfer.getCreatedAt();
        entity.completedAt = transfer.getCompletedAt();
        return entity;
    }

    public Transfer toDomain() {
        return Transfer.builder()
            .transferId(id)
            .idempotencyKey(idempotencyKey)
            .fromWalletId(fromWalletId)
            .toWalletId(toWalletId)
            .coinId(coinId)
            .amount(amount)
            .status(status)
            .createdAt(createdAt)
            .completedAt(completedAt)
            .build();
    }
}
