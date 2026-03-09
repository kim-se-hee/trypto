package ksh.tryptobackend.wallet.adapter.out.repository;

import ksh.tryptobackend.wallet.adapter.out.entity.WalletJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WalletJpaRepository extends JpaRepository<WalletJpaEntity, Long> {

    Optional<WalletJpaEntity> findByRoundIdAndExchangeId(Long roundId, Long exchangeId);

    List<WalletJpaEntity> findByRoundId(Long roundId);

    List<WalletJpaEntity> findByRoundIdIn(List<Long> roundIds);

    List<WalletJpaEntity> findByExchangeId(Long exchangeId);
}
