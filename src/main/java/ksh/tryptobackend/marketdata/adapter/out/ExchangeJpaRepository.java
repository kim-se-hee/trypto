package ksh.tryptobackend.marketdata.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeJpaRepository extends JpaRepository<ExchangeJpaEntity, Long> {
}
