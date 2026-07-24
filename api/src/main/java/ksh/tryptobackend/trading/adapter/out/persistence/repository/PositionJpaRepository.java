package ksh.tryptobackend.trading.adapter.out.persistence.repository;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.trading.adapter.out.persistence.entity.PositionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface PositionJpaRepository extends JpaRepository<PositionJpaEntity, Long> {

    Optional<PositionJpaEntity> findByWalletIdAndCoinId(Long walletId, Long coinId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PositionJpaEntity> findWithLockByWalletIdAndCoinId(Long walletId, Long coinId);

    List<PositionJpaEntity> findByWalletId(Long walletId);
}
