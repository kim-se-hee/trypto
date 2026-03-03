package ksh.tryptobackend.marketdata.adapter.out.repository;

import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinChainJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ExchangeCoinChainJpaRepository extends JpaRepository<ExchangeCoinChainJpaEntity, Long> {

    @Query("SELECT ecc FROM ExchangeCoinChainJpaEntity ecc " +
        "JOIN ExchangeCoinJpaEntity ec ON ecc.exchangeCoinId = ec.id " +
        "WHERE ec.exchangeId = :exchangeId AND ec.coinId = :coinId AND ecc.chain = :chain")
    Optional<ExchangeCoinChainJpaEntity> findByExchangeIdAndCoinIdAndChain(
        Long exchangeId, Long coinId, String chain);
}
