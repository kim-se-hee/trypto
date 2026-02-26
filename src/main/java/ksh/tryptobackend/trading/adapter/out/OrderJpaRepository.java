package ksh.tryptobackend.trading.adapter.out;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    Optional<OrderJpaEntity> findByIdempotencyKey(String idempotencyKey);

    long countByWalletIdAndCreatedAtBetween(Long walletId, LocalDateTime from, LocalDateTime to);
}
