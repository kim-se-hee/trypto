package ksh.tryptobackend.marketdata.adapter.out.repository;

import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExchangeCoinJpaRepository extends JpaRepository<ExchangeCoinJpaEntity, Long> {

    List<ExchangeCoinJpaEntity> findByIdIn(Collection<Long> ids);

    Optional<ExchangeCoinJpaEntity> findByExchangeIdAndCoinId(Long exchangeId, Long coinId);

    boolean existsByExchangeIdAndCoinId(Long exchangeId, Long coinId);

    List<ExchangeCoinJpaEntity> findByExchangeIdAndCoinIdIn(Long exchangeId, Collection<Long> coinIds);

    List<ExchangeCoinJpaEntity> findByExchangeId(Long exchangeId);
}
