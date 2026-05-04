package ksh.tryptobackend.investmentround.adapter.out.repository;

import java.util.Optional;
import java.util.UUID;
import ksh.tryptobackend.investmentround.adapter.out.entity.EmergencyFundingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmergencyFundingJpaRepository
        extends JpaRepository<EmergencyFundingJpaEntity, Long> {

    Optional<EmergencyFundingJpaEntity> findByRoundIdAndIdempotencyKey(
            Long roundId, UUID idempotencyKey);
}
