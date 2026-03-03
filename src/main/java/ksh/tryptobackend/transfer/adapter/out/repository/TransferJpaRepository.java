package ksh.tryptobackend.transfer.adapter.out.repository;

import ksh.tryptobackend.transfer.adapter.out.entity.TransferJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, Long> {

    Optional<TransferJpaEntity> findByIdempotencyKey(UUID idempotencyKey);

    Page<TransferJpaEntity> findByFromWalletId(Long fromWalletId, Pageable pageable);

    Page<TransferJpaEntity> findByToWalletId(Long toWalletId, Pageable pageable);

    @Query("SELECT t FROM TransferJpaEntity t " +
        "WHERE t.fromWalletId = :walletId OR t.toWalletId = :walletId")
    Page<TransferJpaEntity> findByFromWalletIdOrToWalletId(Long walletId, Pageable pageable);
}
