package ksh.tryptobackend.transfer.adapter.out.repository;

import ksh.tryptobackend.transfer.adapter.out.entity.TransferJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, Long> {

    Optional<TransferJpaEntity> findByIdempotencyKey(UUID idempotencyKey);
}
