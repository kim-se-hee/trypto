package ksh.tryptobackend.ranking.adapter.out;

import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.ranking.adapter.out.entity.PortfolioSnapshotJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.QPortfolioSnapshotJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.QRankingCoinJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.QRankingExchangeJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.QSnapshotDetailJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.SnapshotDetailJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.repository.PortfolioSnapshotJpaRepository;
import ksh.tryptobackend.ranking.adapter.out.repository.SnapshotDetailJpaRepository;
import ksh.tryptobackend.ranking.application.port.out.PortfolioSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.SnapshotPersistencePort;
import ksh.tryptobackend.ranking.application.port.out.SnapshotQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.SnapshotDetailProjection;
import ksh.tryptobackend.ranking.application.port.out.dto.SnapshotInfo;
import ksh.tryptobackend.ranking.domain.model.PortfolioSnapshot;
import ksh.tryptobackend.ranking.domain.model.SnapshotDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PortfolioSnapshotJpaPersistenceAdapter implements PortfolioSnapshotPort, SnapshotQueryPort, SnapshotPersistencePort {

    private final JPAQueryFactory queryFactory;
    private final PortfolioSnapshotJpaRepository snapshotRepository;
    private final SnapshotDetailJpaRepository detailRepository;

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

    public PortfolioSnapshot save(PortfolioSnapshot domain) {
        PortfolioSnapshotJpaEntity entity = PortfolioSnapshotJpaEntity.fromDomain(domain);
        PortfolioSnapshotJpaEntity saved = snapshotRepository.save(entity);
        return saved.toDomain();
    }

    public void saveDetails(Long snapshotId, List<SnapshotDetail> details) {
        List<SnapshotDetailJpaEntity> entities = details.stream()
            .map(d -> SnapshotDetailJpaEntity.fromDomain(d, snapshotId))
            .toList();
        detailRepository.saveAll(entities);
    }

    private Expression<LocalDate> latestSnapshotDate(Long userId, Long roundId) {
        return JPAExpressions
            .select(snapshot.snapshotDate.max())
            .from(snapshot)
            .where(snapshot.userId.eq(userId)
                .and(snapshot.roundId.eq(roundId)));
    }
}
