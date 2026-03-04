package ksh.tryptobackend.wallet.adapter.out.repository;

import jakarta.persistence.LockModeType;
import ksh.tryptobackend.wallet.adapter.out.entity.WalletBalanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface WalletBalanceJpaRepository extends JpaRepository<WalletBalanceJpaEntity, Long> {

    Optional<WalletBalanceJpaEntity> findByWalletIdAndCoinId(Long walletId, Long coinId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WalletBalanceJpaEntity> findForUpdateByWalletIdAndCoinId(Long walletId, Long coinId);
}
