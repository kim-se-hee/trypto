package ksh.tryptobackend.wallet.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import ksh.tryptobackend.wallet.domain.model.WalletBalance;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "wallet_balance",
        uniqueConstraints = @UniqueConstraint(columnNames = {"wallet_id", "coin_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WalletBalanceJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "balance_id")
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "coin_id", nullable = false)
    private Long coinId;

    @Column(name = "available", nullable = false, precision = 30, scale = 8)
    private BigDecimal available;

    @Column(name = "locked", nullable = false, precision = 30, scale = 8)
    private BigDecimal locked;

    public WalletBalanceJpaEntity(
            Long walletId, Long coinId, BigDecimal available, BigDecimal locked) {
        this.walletId = walletId;
        this.coinId = coinId;
        this.available = available;
        this.locked = locked;
    }

    public WalletBalance toDomain() {
        return WalletBalance.builder()
                .id(id)
                .walletId(walletId)
                .coinId(coinId)
                .available(available)
                .locked(locked)
                .build();
    }

    public void updateFrom(WalletBalance balance) {
        this.available = balance.getAvailable();
        this.locked = balance.getLocked();
    }
}
