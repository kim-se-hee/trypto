package ksh.tryptobackend.wallet.adapter.out.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import ksh.tryptobackend.wallet.domain.model.DepositAddress;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "deposit_address",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_deposit_address_wallet_coin", columnNames = {"wallet_id", "coin_id"})
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DepositAddressJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "deposit_address_id")
    private Long id;

    @Column(name = "wallet_id", nullable = false)
    private Long walletId;

    @Column(name = "coin_id", nullable = false)
    private Long coinId;

    @Column(name = "address", nullable = false, length = 255)
    private String address;

    public static DepositAddressJpaEntity fromDomain(DepositAddress domain) {
        DepositAddressJpaEntity entity = new DepositAddressJpaEntity();
        entity.id = domain.getDepositAddressId();
        entity.walletId = domain.getWalletId();
        entity.coinId = domain.getCoinId();
        entity.address = domain.getAddress();
        return entity;
    }

    public DepositAddress toDomain() {
        return DepositAddress.builder()
            .depositAddressId(id)
            .walletId(walletId)
            .coinId(coinId)
            .address(address)
            .build();
    }
}
