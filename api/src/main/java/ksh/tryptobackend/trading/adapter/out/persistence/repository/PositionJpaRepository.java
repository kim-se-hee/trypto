package ksh.tryptobackend.trading.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.trading.adapter.out.persistence.entity.PositionJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PositionJpaRepository extends JpaRepository<PositionJpaEntity, Long> {

    Optional<PositionJpaEntity> findByWalletIdAndCoinId(Long walletId, Long coinId);

    List<PositionJpaEntity> findByWalletId(Long walletId);
}
