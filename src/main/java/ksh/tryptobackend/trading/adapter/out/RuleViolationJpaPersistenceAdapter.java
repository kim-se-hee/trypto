package ksh.tryptobackend.trading.adapter.out;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.trading.adapter.out.entity.QOrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.entity.QRuleViolationJpaEntity;
import ksh.tryptobackend.trading.adapter.out.entity.RuleViolationJpaEntity;
import ksh.tryptobackend.trading.adapter.out.repository.RuleViolationJpaRepository;
import ksh.tryptobackend.trading.application.port.out.ViolationPersistencePort;
import ksh.tryptobackend.trading.application.port.out.ViolationQueryPort;
import ksh.tryptobackend.trading.application.port.out.dto.ViolationInfo;
import ksh.tryptobackend.trading.domain.model.RuleViolation;
import ksh.tryptobackend.wallet.adapter.out.entity.QWalletJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RuleViolationJpaPersistenceAdapter implements ViolationPersistencePort, ViolationQueryPort {

    private final RuleViolationJpaRepository repository;
    private final JPAQueryFactory queryFactory;

    private static final QRuleViolationJpaEntity violation = QRuleViolationJpaEntity.ruleViolationJpaEntity;
    private static final QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;
    private static final QWalletJpaEntity wallet = QWalletJpaEntity.walletJpaEntity;

    @Override
    public void saveAll(Long orderId, List<RuleViolation> violations) {
        List<RuleViolationJpaEntity> entities = violations.stream()
            .map(v -> RuleViolationJpaEntity.fromOrderViolation(orderId, v))
            .toList();
        repository.saveAll(entities);
    }

    @Override
    public List<ViolationInfo> findByRuleIdsAndExchangeId(List<Long> ruleIds, Long exchangeId) {
        if (ruleIds.isEmpty()) {
            return Collections.emptyList();
        }

        return queryFactory
            .select(Projections.constructor(ViolationInfo.class,
                violation.id,
                violation.orderId,
                violation.ruleId,
                violation.createdAt))
            .from(violation)
            .leftJoin(order).on(violation.orderId.eq(order.id))
            .leftJoin(wallet).on(order.walletId.eq(wallet.id))
            .where(
                violation.ruleId.in(ruleIds),
                wallet.exchangeId.eq(exchangeId)
                    .or(violation.orderId.isNull())
            )
            .fetch();
    }
}
