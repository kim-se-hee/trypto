package ksh.tryptobackend.wallet.adapter.out.persistence.repository;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.wallet.adapter.out.persistence.entity.WalletJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, Long> {

    Optional<WalletJpaEntity> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<WalletJpaEntity> findByRoundId(Long roundId);

    List<WalletJpaEntity> findByRoundIdIn(List<Long> roundIds);

    List<WalletJpaEntity> findByExchangeId(Long exchangeId);
}
