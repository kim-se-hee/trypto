package ksh.tryptobackend.marketdata.adapter.out.repository;

import java.util.Optional;
import ksh.tryptobackend.marketdata.adapter.out.entity.WithdrawalFeeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawalFeeJpaRepository extends JpaRepository<WithdrawalFeeJpaEntity, Long> {

    Optional<WithdrawalFeeJpaEntity> findByExchangeIdAndCoinIdAndChain(
            Long exchangeId, Long coinId, String chain);
}
