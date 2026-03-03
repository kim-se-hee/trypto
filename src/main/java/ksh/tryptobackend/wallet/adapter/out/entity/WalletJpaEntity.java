package ksh.tryptobackend.wallet.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import ksh.tryptobackend.wallet.domain.model.Wallet;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "wallet",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_wallet_round_exchange", columnNames = {"round_id", "exchange_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long id;

    @Column(name = "round_id", nullable = false)
    private Long roundId;

    @Column(name = "exchange_id", nullable = false)
    private Long exchangeId;

    @Column(name = "seed_amount", nullable = false, precision = 30, scale = 8)
    private BigDecimal seedAmount;

    @Column(name = "wallet_address", length = 255)
    private String walletAddress;

    @Column(name = "wallet_tag", length = 255)
    private String walletTag;

    @Column(name = "chain", length = 50)
    private String chain;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private WalletJpaEntity(Long roundId, Long exchangeId, BigDecimal seedAmount, LocalDateTime createdAt) {
        this.roundId = roundId;
        this.exchangeId = exchangeId;
        this.seedAmount = seedAmount;
        this.createdAt = createdAt;
    }

    public static WalletJpaEntity fromDomain(Wallet wallet) {
        return new WalletJpaEntity(wallet.getRoundId(), wallet.getExchangeId(),
            wallet.getSeedAmount(), wallet.getCreatedAt());
    }
}
