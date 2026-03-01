package ksh.tryptobackend.ranking.adapter.out;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.ranking.adapter.out.entity.QPortfolioSnapshotJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.QRankingCoinJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.QRankingExchangeJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.QSnapshotDetailJpaEntity;
import ksh.tryptobackend.ranking.application.port.out.PortfolioSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.SnapshotQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.SnapshotDetailProjection;
import ksh.tryptobackend.ranking.application.port.out.dto.SnapshotInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PortfolioSnapshotJpaPersistenceAdapter implements PortfolioSnapshotPort, SnapshotQueryPort {

    private final JPAQueryFactory queryFactory;

    private static final QPortfolioSnapshotJpaEntity snapshot = QPortfolioSnapshotJpaEntity.portfolioSnapshotJpaEntity;
    private static final QSnapshotDetailJpaEntity detail = QSnapshotDetailJpaEntity.snapshotDetailJpaEntity;
    private static final QRankingCoinJpaEntity coin = QRankingCoinJpaEntity.rankingCoinJpaEntity;
    private static final QRankingExchangeJpaEntity exchange = QRankingExchangeJpaEntity.rankingExchangeJpaEntity;

    @Override
    public List<SnapshotDetailProjection> findLatestSnapshotDetails(Long userId, Long roundId) {
        return queryFactory
            .select(Projections.constructor(SnapshotDetailProjection.class,
                coin.symbol,
                exchange.name,
                detail.assetRatio,
                detail.profitRate))
            .from(detail)
            .join(snapshot).on(detail.snapshotId.eq(snapshot.id))
            .join(coin).on(detail.coinId.eq(coin.id))
            .join(exchange).on(snapshot.exchangeId.eq(exchange.id))
            .where(snapshot.userId.eq(userId)
                .and(snapshot.roundId.eq(roundId))
                .and(snapshot.snapshotDate.eq(latestSnapshotDate(userId, roundId))))
            .fetch();
    }

    @Override
    public Optional<SnapshotInfo> findLatestByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        SnapshotInfo result = queryFactory
            .select(Projections.constructor(SnapshotInfo.class,
                snapshot.id, snapshot.roundId, snapshot.exchangeId,
                snapshot.totalAsset, snapshot.totalInvestment,
                snapshot.totalProfitRate, snapshot.snapshotDate))
            .from(snapshot)
            .where(
                snapshot.roundId.eq(roundId),
                snapshot.exchangeId.eq(exchangeId)
            )
            .orderBy(snapshot.snapshotDate.desc())
            .limit(1)
            .fetchOne();
        return Optional.ofNullable(result);
    }

    @Override
    public List<SnapshotInfo> findAllByRoundIdAndExchangeId(Long roundId, Long exchangeId) {
        return queryFactory
            .select(Projections.constructor(SnapshotInfo.class,
                snapshot.id, snapshot.roundId, snapshot.exchangeId,
                snapshot.totalAsset, snapshot.totalInvestment,
                snapshot.totalProfitRate, snapshot.snapshotDate))
            .from(snapshot)
            .where(
                snapshot.roundId.eq(roundId),
                snapshot.exchangeId.eq(exchangeId)
            )
            .orderBy(snapshot.snapshotDate.asc())
            .fetch();
    }

    private Expression<LocalDateTime> latestSnapshotDate(Long userId, Long roundId) {
        return JPAExpressions
            .select(snapshot.snapshotDate.max())
            .from(snapshot)
            .where(snapshot.userId.eq(userId)
                .and(snapshot.roundId.eq(roundId)));
    }
}
