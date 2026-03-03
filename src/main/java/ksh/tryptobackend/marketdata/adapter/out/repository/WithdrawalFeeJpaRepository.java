package ksh.tryptobackend.marketdata.adapter.out.repository;

import ksh.tryptobackend.marketdata.adapter.out.entity.WithdrawalFeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WithdrawalFeeJpaRepository extends JpaRepository<WithdrawalFeeJpaEntity, Long> {

    Optional<WithdrawalFeeJpaEntity> findByExchangeIdAndCoinIdAndChain(
        Long exchangeId, Long coinId, String chain);
}
