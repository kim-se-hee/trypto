package ksh.tryptobackend.marketdata.adapter.out.repository;

import ksh.tryptobackend.marketdata.adapter.out.entity.CoinJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CoinJpaRepository extends JpaRepository<CoinJpaEntity, Long> {

    List<CoinJpaEntity> findByIdIn(Collection<Long> ids);
}
