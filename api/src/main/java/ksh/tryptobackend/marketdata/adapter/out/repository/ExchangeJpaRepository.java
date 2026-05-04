package ksh.tryptobackend.marketdata.adapter.out.repository;

import java.util.Optional;
import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExchangeJpaRepository extends JpaRepository<ExchangeJpaEntity, Long> {

    Optional<ExchangeJpaEntity> findByName(String name);
}
