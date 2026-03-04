package ksh.tryptobackend.wallet.adapter.out.entity;

import jakarta.persistence.*;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet_balance")
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

    public WalletBalanceJpaEntity(Long walletId, Long coinId, BigDecimal available, BigDecimal locked) {
        this.walletId = walletId;
        this.coinId = coinId;
        this.available = available;
        this.locked = locked;
    }

    public void deductAvailable(BigDecimal amount) {
        BigDecimal result = available.subtract(amount);
        if (result.signum() < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.available = result;
    }

    public void addAvailable(BigDecimal amount) {
        this.available = available.add(amount);
    }

    public void lock(BigDecimal amount) {
        BigDecimal result = available.subtract(amount);
        if (result.signum() < 0) {
            throw new CustomException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.available = result;
        this.locked = locked.add(amount);
    }

    public void unlock(BigDecimal amount) {
        this.locked = locked.subtract(amount);
        this.available = available.add(amount);
    }
}
