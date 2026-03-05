package ksh.tryptobackend.investmentround.adapter.out;

import ksh.tryptobackend.investmentround.adapter.out.entity.EmergencyFundingJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.EmergencyFundingJpaRepository;
import ksh.tryptobackend.investmentround.application.port.out.EmergencyFundingPersistencePort;
import ksh.tryptobackend.investmentround.application.port.out.EmergencyFundingQueryPort;
import ksh.tryptobackend.investmentround.domain.model.EmergencyFunding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmergencyFundingJpaPersistenceAdapter implements EmergencyFundingPersistencePort, EmergencyFundingQueryPort {

    private final EmergencyFundingJpaRepository repository;

    @Override
    public EmergencyFunding save(EmergencyFunding funding) {
        EmergencyFundingJpaEntity saved = repository.save(EmergencyFundingJpaEntity.fromDomain(funding));
        return saved.toDomain();
    }

    @Override
    public Optional<EmergencyFunding> findByRoundIdAndIdempotencyKey(Long roundId, UUID idempotencyKey) {
        return repository.findByRoundIdAndIdempotencyKey(roundId, idempotencyKey)
            .map(EmergencyFundingJpaEntity::toDomain);
    }

    @Override
    public BigDecimal sumAmountByRoundId(Long roundId) {
        return repository.sumAmountByRoundId(roundId);
    }

    @Override
    public BigDecimal sumAmountByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return repository.sumAmountByRoundIdAndExchangeId(roundId, exchangeId);
    }
}
