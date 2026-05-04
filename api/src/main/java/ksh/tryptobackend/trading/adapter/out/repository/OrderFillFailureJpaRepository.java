package ksh.tryptobackend.trading.adapter.out.repository;

import java.util.List;
import ksh.tryptobackend.trading.adapter.out.entity.OrderFillFailureJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderFillFailureJpaRepository
        extends JpaRepository<OrderFillFailureJpaEntity, Long> {

    List<OrderFillFailureJpaEntity> findByResolvedFalse();
}
