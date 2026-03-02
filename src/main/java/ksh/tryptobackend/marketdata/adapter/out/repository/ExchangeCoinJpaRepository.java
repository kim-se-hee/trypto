package ksh.tryptobackend.marketdata.adapter.out.repository;

import ksh.tryptobackend.marketdata.adapter.out.entity.ExchangeCoinJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ExchangeCoinJpaRepository extends JpaRepository<ExchangeCoinJpaEntity, Long> {

    List<ExchangeCoinJpaEntity> findByIdIn(Collection<Long> ids);
}
