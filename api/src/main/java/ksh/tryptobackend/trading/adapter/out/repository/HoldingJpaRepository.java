package ksh.tryptobackend.trading.adapter.out.repository;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.trading.adapter.out.entity.HoldingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoldingJpaRepository extends JpaRepository<HoldingJpaEntity, Long> {

    Optional<HoldingJpaEntity> findByWalletIdAndCoinId(Long walletId, Long coinId);

    List<HoldingJpaEntity> findByWalletId(Long walletId);
}
