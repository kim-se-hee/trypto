package ksh.tryptobackend.wallet.adapter.out.repository;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletBalanceJpaRepository extends JpaRepository<WalletBalanceJpaEntity, Long> {

    List<WalletBalanceJpaEntity> findByWalletId(Long walletId);

    Optional<WalletBalanceJpaEntity> findByWalletIdAndCoinId(Long walletId, Long coinId);
}
