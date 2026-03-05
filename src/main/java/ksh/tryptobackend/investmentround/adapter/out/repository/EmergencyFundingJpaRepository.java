package ksh.tryptobackend.investmentround.adapter.out.repository;

import ksh.tryptobackend.investmentround.adapter.out.entity.EmergencyFundingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface EmergencyFundingJpaRepository extends JpaRepository<EmergencyFundingJpaEntity, Long> {

    Optional<EmergencyFundingJpaEntity> findByRoundIdAndIdempotencyKey(Long roundId, UUID idempotencyKey);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM EmergencyFundingJpaEntity e WHERE e.roundId = :roundId")
    BigDecimal sumAmountByRoundId(Long roundId);

    @Query("SELECT COALESCE(SUM(e.amount), 0) FROM EmergencyFundingJpaEntity e WHERE e.roundId = :roundId AND e.exchangeId = :exchangeId")
    BigDecimal sumAmountByRoundIdAndExchangeId(Long roundId, Long exchangeId);
}
