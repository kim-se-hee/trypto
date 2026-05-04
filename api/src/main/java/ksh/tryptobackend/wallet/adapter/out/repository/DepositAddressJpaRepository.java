package ksh.tryptobackend.wallet.adapter.out.repository;

import java.util.Optional;
import ksh.tryptobackend.wallet.adapter.out.entity.DepositAddressJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DepositAddressJpaRepository extends JpaRepository<DepositAddressJpaEntity, Long> {

    Optional<DepositAddressJpaEntity> findByWalletIdAndCoinId(Long walletId, Long coinId);
}
