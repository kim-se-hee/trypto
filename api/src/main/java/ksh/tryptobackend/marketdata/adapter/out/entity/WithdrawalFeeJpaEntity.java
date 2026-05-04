package ksh.tryptobackend.marketdata.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import ksh.tryptobackend.marketdata.domain.model.WithdrawalFee;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "withdrawal_fee")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawalFeeJpaEntity {

    @Id
    @Column(name = "withdrawal_fee_id")
    private Long id;

    @Column(name = "exchange_id", nullable = false)
    private Long exchangeId;

    @Column(name = "coin_id", nullable = false)
    private Long coinId;

    @Column(name = "chain", nullable = false, length = 50)
    private String chain;

    @Column(name = "fee", nullable = false, precision = 30, scale = 8)
    private BigDecimal fee;

    @Column(name = "min_withdrawal", nullable = false, precision = 30, scale = 8)
    private BigDecimal minWithdrawal;

    public WithdrawalFee toDomain() {
        return WithdrawalFee.builder()
                .withdrawalFeeId(id)
                .exchangeId(exchangeId)
                .coinId(coinId)
                .chain(chain)
                .fee(fee)
                .minWithdrawal(minWithdrawal)
                .build();
    }
}
