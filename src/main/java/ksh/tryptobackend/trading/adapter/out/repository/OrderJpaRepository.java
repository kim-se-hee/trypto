package ksh.tryptobackend.trading.adapter.out.repository;

import ksh.tryptobackend.trading.adapter.out.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    Optional<OrderJpaEntity> findByIdempotencyKey(String idempotencyKey);

    long countByWalletIdAndCreatedAtBetween(Long walletId, LocalDateTime from, LocalDateTime to);

    boolean existsByWalletIdAndStatus(Long walletId, OrderStatus status);

    int countByWalletIdAndStatus(Long walletId, OrderStatus status);
}
