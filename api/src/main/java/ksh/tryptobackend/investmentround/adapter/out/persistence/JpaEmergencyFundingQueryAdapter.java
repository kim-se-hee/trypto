package ksh.tryptobackend.investmentround.adapter.out.persistence;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.math.BigDecimal;
import ksh.tryptobackend.investmentround.adapter.out.persistence.entity.QEmergencyFundingJpaEntity;
import ksh.tryptobackend.investmentround.application.port.out.EmergencyFundingQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JpaEmergencyFundingQueryAdapter implements EmergencyFundingQueryPort {

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
                .where(e.roundId.eq(roundId), e.exchangeId.eq(exchangeId))
                .fetchOne();
        return result != null ? result : BigDecimal.ZERO;
    }
}
