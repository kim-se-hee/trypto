package ksh.tryptobackend.investmentround.adapter.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.investmentround.adapter.out.entity.EmergencyFundingJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.entity.QEmergencyFundingJpaEntity;
import ksh.tryptobackend.investmentround.adapter.out.repository.EmergencyFundingJpaRepository;
import ksh.tryptobackend.investmentround.application.port.out.EmergencyFundingQueryPort;
import ksh.tryptobackend.investmentround.domain.model.EmergencyFunding;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class EmergencyFundingQueryAdapter implements EmergencyFundingQueryPort {

    private final EmergencyFundingJpaRepository repository;
    private final JPAQueryFactory queryFactory;

    @Override
    public BigDecimal sumAmountByRoundId(Long roundId) {
        QEmergencyFundingJpaEntity e = QEmergencyFundingJpaEntity.emergencyFundingJpaEntity;
        BigDecimal result = queryFactory
            .select(e.amount.sum().coalesce(BigDecimal.ZERO))
            .from(e)
            .where(e.roundId.eq(roundId))
            .fetchOne();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public BigDecimal sumAmountByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        QEmergencyFundingJpaEntity e = QEmergencyFundingJpaEntity.emergencyFundingJpaEntity;
        BigDecimal result = queryFactory
            .select(e.amount.sum().coalesce(BigDecimal.ZERO))
            .from(e)
            .where(
                e.roundId.eq(roundId),
                e.exchangeId.eq(exchangeId)
            )
            .fetchOne();
        return result != null ? result : BigDecimal.ZERO;
    }

    @Override
    public Optional<EmergencyFunding> findByRoundIdAndIdempotencyKey(Long roundId, UUID idempotencyKey) {
        return repository.findByRoundIdAndIdempotencyKey(roundId, idempotencyKey)
            .map(EmergencyFundingJpaEntity::toDomain);
    }
}
