package ksh.tryptobackend.trading.adapter.out.persistence.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Optional;
import ksh.tryptobackend.trading.adapter.out.persistence.entity.OrderJpaEntity;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    Optional<OrderJpaEntity> findByIdempotencyKey(String idempotencyKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<OrderJpaEntity> findWithLockById(Long orderId);

    long countByWalletIdAndCreatedAtBetween(Long walletId, LocalDateTime from, LocalDateTime to);

    boolean existsByWalletIdAndStatus(Long walletId, OrderStatus status);

    int countByWalletIdAndStatus(Long walletId, OrderStatus status);
}
