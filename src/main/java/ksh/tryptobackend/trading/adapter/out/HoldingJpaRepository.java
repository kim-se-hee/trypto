package ksh.tryptobackend.trading.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HoldingJpaRepository extends JpaRepository<HoldingJpaEntity, Long> {

    Optional<HoldingJpaEntity> findByWalletIdAndCoinId(Long walletId, Long coinId);
}
