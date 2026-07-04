package ksh.tryptobackend.investmentround.adapter.out.repository;

import java.util.Optional;
import java.util.UUID;
import ksh.tryptobackend.investmentround.adapter.out.entity.IdempotencyKeyJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IdempotencyKeyJpaRepository extends JpaRepository<IdempotencyKeyJpaEntity, Long> {

    Optional<IdempotencyKeyJpaEntity> findByIdempotencyKey(UUID idempotencyKey);
}
