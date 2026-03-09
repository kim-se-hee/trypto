package ksh.tryptobackend.portfolio.adapter.out;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.portfolio.adapter.out.entity.QPortfolioSnapshotJpaEntity;
import ksh.tryptobackend.portfolio.adapter.out.entity.QSnapshotDetailJpaEntity;
import ksh.tryptobackend.portfolio.application.port.out.PortfolioSnapshotQueryPort;
import ksh.tryptobackend.portfolio.application.port.out.dto.SnapshotDetailProjection;
import ksh.tryptobackend.portfolio.application.port.out.dto.SnapshotInfo;
import ksh.tryptobackend.portfolio.application.port.out.dto.UserSnapshotSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PortfolioSnapshotQueryAdapter implements PortfolioSnapshotQueryPort {

    private final JPAQueryFactory queryFactory;

    private static final QPortfolioSnapshotJpaEntity snapshot = QPortfolioSnapshotJpaEntity.portfolioSnapshotJpaEntity;
    private static final QSnapshotDetailJpaEntity detail = QSnapshotDetailJpaEntity.snapshotDetailJpaEntity;

    @Override
    public List<SnapshotDetailProjection> findLatestSnapshotDetails(Long userId, Long roundId) {
        return queryFactory
            .select(Projections.constructor(SnapshotDetailProjection.class,
                detail.coinId,
                snapshot.exchangeId,
                detail.assetRatio,
                detail.profitRate))
            .from(detail)
            .join(snapshot).on(detail.snapshotId.eq(snapshot.id))
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

    @Override
    public List<UserSnapshotSummary> findLatestSummaries(LocalDate snapshotDate) {
        return queryFactory
            .select(Projections.constructor(UserSnapshotSummary.class,
                snapshot.userId,
                snapshot.roundId,
                snapshot.totalAssetKrw.sum(),
                snapshot.totalInvestmentKrw.sum()))
            .from(snapshot)
            .where(snapshot.snapshotDate.eq(snapshotDate))
            .groupBy(snapshot.userId, snapshot.roundId)
            .fetch();
    }

    private Expression<LocalDate> latestSnapshotDate(Long userId, Long roundId) {
        return JPAExpressions
            .select(snapshot.snapshotDate.max())
            .from(snapshot)
            .where(snapshot.userId.eq(userId)
                .and(snapshot.roundId.eq(roundId)));
    }
}
