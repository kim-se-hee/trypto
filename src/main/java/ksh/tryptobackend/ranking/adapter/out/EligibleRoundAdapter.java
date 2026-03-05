package ksh.tryptobackend.ranking.adapter.out;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.investmentround.adapter.out.entity.QInvestmentRoundJpaEntity;
import ksh.tryptobackend.investmentround.domain.vo.RoundStatus;
import ksh.tryptobackend.ranking.application.port.out.EligibleRoundQueryPort;
import ksh.tryptobackend.ranking.domain.vo.EligibleRound;
import ksh.tryptobackend.trading.adapter.out.entity.QOrderJpaEntity;
import ksh.tryptobackend.trading.domain.vo.OrderStatus;
import ksh.tryptobackend.wallet.adapter.out.entity.QWalletJpaEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EligibleRoundAdapter implements EligibleRoundQueryPort {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<EligibleRound> findAll() {
        QInvestmentRoundJpaEntity round = QInvestmentRoundJpaEntity.investmentRoundJpaEntity;
        QWalletJpaEntity wallet = QWalletJpaEntity.walletJpaEntity;
        QOrderJpaEntity order = QOrderJpaEntity.orderJpaEntity;

        return queryFactory
            .select(Projections.constructor(EligibleRound.class,
                round.userId,
                round.id,
                order.id.count().intValue(),
                round.startedAt
            ))
            .from(round)
            .leftJoin(wallet).on(wallet.roundId.eq(round.id))
            .leftJoin(order).on(order.walletId.eq(wallet.id)
                .and(order.status.eq(OrderStatus.FILLED)))
            .where(round.status.eq(RoundStatus.ACTIVE))
            .groupBy(round.id, round.userId, round.startedAt)
            .fetch();
    }
}
