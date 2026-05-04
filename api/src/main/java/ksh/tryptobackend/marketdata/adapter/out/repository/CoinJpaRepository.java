package ksh.tryptobackend.marketdata.adapter.out.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.marketdata.adapter.out.entity.CoinJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoinJpaRepository extends JpaRepository<CoinJpaEntity, Long> {

    List<CoinJpaEntity> findByIdIn(Collection<Long> ids);

    Optional<CoinJpaEntity> findBySymbol(String symbol);
}
