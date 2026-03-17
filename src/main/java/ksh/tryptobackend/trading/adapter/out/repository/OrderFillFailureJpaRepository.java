package ksh.tryptobackend.trading.adapter.out.repository;

import ksh.tryptobackend.trading.adapter.out.entity.OrderFillFailureJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderFillFailureJpaRepository extends JpaRepository<OrderFillFailureJpaEntity, Long> {

    List<OrderFillFailureJpaEntity> findByResolvedFalse();
}
