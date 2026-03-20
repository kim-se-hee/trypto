package ksh.tryptobackend.wallet.adapter.out.repository;

import ksh.tryptobackend.wallet.adapter.out.entity.DepositAddressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DepositAddressJpaRepository extends JpaRepository<DepositAddressJpaEntity, Long> {

    Optional<DepositAddressJpaEntity> findByWalletIdAndCoinId(Long walletId, Long coinId);
}
