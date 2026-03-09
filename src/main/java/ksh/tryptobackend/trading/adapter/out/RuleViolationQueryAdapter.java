package ksh.tryptobackend.trading.adapter.out;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.trading.adapter.out.entity.QOrderJpaEntity;
import ksh.tryptobackend.trading.adapter.out.entity.QRuleViolationJpaEntity;
import ksh.tryptobackend.trading.application.port.out.RuleViolationQueryPort;
import ksh.tryptobackend.trading.domain.vo.RuleViolationRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RuleViolationQueryAdapter implements RuleViolationQueryPort {

    private final JPAQueryFactory queryFactory;

    private static final QRuleViolationJpaEntity violation = QRuleViolationJpaEntity.ruleViolationJpaEntity;
    private static final QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;

    @Override
    public List<RuleViolationRef> findByRuleIdsAndWalletIds(List<Long> ruleIds, List<Long> walletIds) {
        if (ruleIds.isEmpty()) {
            return Collections.emptyList();
        }

        return queryFactory
            .select(Projections.constructor(RuleViolationRef.class,
                violation.id,
                violation.orderId,
                violation.ruleId,
                violation.createdAt))
            .from(violation)
            .leftJoin(order).on(violation.orderId.eq(order.id))
            .where(
                violation.ruleId.in(ruleIds),
                order.walletId.in(walletIds)
                    .or(violation.orderId.isNull())
            )
            .fetch();
    }
}
