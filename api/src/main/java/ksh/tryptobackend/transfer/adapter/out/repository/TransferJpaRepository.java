package ksh.tryptobackend.transfer.adapter.out.repository;

import java.util.Optional;
import java.util.UUID;
import ksh.tryptobackend.transfer.adapter.out.entity.TransferJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferJpaRepository extends JpaRepository<TransferJpaEntity, Long> {

    Optional<TransferJpaEntity> findByIdempotencyKey(UUID idempotencyKey);
}
