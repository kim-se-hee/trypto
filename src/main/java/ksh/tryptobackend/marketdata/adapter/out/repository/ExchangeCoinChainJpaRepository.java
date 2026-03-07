package ksh.tryptobackend.marketdata.adapter.out.repository;

import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinChainJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeCoinChainJpaRepository extends JpaRepository<ExchangeCoinChainJpaEntity, Long> {
}
