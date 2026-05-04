package ksh.tryptobackend.trading.adapter.out.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import ksh.tryptobackend.trading.adapter.out.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    Optional<OrderJpaEntity> findByIdempotencyKey(String idempotencyKey);

    long countByWalletIdAndCreatedAtBetween(Long walletId, LocalDateTime from, LocalDateTime to);

    boolean existsByWalletIdAndStatus(Long walletId, OrderStatus status);

    int countByWalletIdAndStatus(Long walletId, OrderStatus status);
}
