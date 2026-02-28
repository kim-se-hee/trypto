package ksh.tryptobackend.ranking.adapter.out;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.ranking.adapter.out.entity.QPortfolioSnapshotJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.QRankingCoinJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.QRankingExchangeJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.QSnapshotDetailJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.repository.PortfolioSnapshotJpaRepository;
import ksh.tryptobackend.ranking.application.port.out.PortfolioSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.dto.SnapshotDetailProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class PortfolioSnapshotJpaPersistenceAdapter implements PortfolioSnapshotPort {

    private final PortfolioSnapshotJpaRepository snapshotRepository;
    private final JPAQueryFactory queryFactory;

    private static final QPortfolioSnapshotJpaEntity snapshot = QPortfolioSnapshotJpaEntity.portfolioSnapshotJpaEntity;
    private static final QSnapshotDetailJpaEntity detail = QSnapshotDetailJpaEntity.snapshotDetailJpaEntity;
    private static final QRankingCoinJpaEntity coin = QRankingCoinJpaEntity.rankingCoinJpaEntity;
    private static final QRankingExchangeJpaEntity exchange = QRankingExchangeJpaEntity.rankingExchangeJpaEntity;

    @Override
    public List<SnapshotDetailProjection> findLatestSnapshotDetails(Long userId, Long roundId) {
        return snapshotRepository.findTopByUserIdAndRoundIdOrderBySnapshotDateDesc(userId, roundId)
            .map(snapshotEntity -> findDetailsBySnapshotId(snapshotEntity.getId()))
            .orElse(Collections.emptyList());
    }

    private List<SnapshotDetailProjection> findDetailsBySnapshotId(Long snapshotId) {
        return queryFactory
            .select(Projections.constructor(SnapshotDetailProjection.class,
                coin.symbol,
                exchange.name,
                detail.assetRatio,
                detail.profitRate))
            .from(detail)
            .join(coin).on(detail.coinId.eq(coin.id))
            .join(exchange).on(detail.exchangeId.eq(exchange.id))
            .where(detail.snapshotId.eq(snapshotId))
            .fetch();
    }
}
