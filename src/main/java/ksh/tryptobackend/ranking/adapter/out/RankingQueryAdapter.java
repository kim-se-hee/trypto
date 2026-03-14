package ksh.tryptobackend.ranking.adapter.out;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.ranking.adapter.out.entity.QRankingJpaEntity;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
import ksh.tryptobackend.ranking.domain.vo.RankingSummary;
import ksh.tryptobackend.ranking.application.port.out.dto.RankingStatsProjection;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RankingQueryAdapter implements RankingQueryPort {

    private final JPAQueryFactory queryFactory;

    private static final QRankingJpaEntity ranking = QRankingJpaEntity.rankingJpaEntity;

    @Override
    public Optional<LocalDate> findLatestReferenceDate(RankingPeriod period) {
        LocalDate result = queryFactory
            .select(ranking.referenceDate.max())
            .from(ranking)
            .where(ranking.period.eq(period))
            .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<RankingSummary> findRankings(RankingPeriod period, LocalDate referenceDate, Integer cursorRank, int size) {
        return queryFactory
            .select(Projections.constructor(RankingSummary.class,
                ranking.rank,
                ranking.userId,
                ranking.profitRate,
                ranking.tradeCount))
            .from(ranking)
            .where(ranking.period.eq(period)
                .and(ranking.referenceDate.eq(referenceDate)),
                cursorRankGt(cursorRank))
            .orderBy(ranking.rank.asc())
            .limit(size)
            .fetch();
    }

    @Override
    public Optional<RankingSummary> findByUserIdAndPeriodAndReferenceDate(Long userId, RankingPeriod period, LocalDate referenceDate) {
        RankingSummary result = queryFactory
            .select(Projections.constructor(RankingSummary.class,
                ranking.rank,
                ranking.userId,
                ranking.profitRate,
                ranking.tradeCount))
            .from(ranking)
            .where(ranking.userId.eq(userId)
                .and(ranking.period.eq(period))
                .and(ranking.referenceDate.eq(referenceDate)))
            .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public RankingStatsProjection getRankingStats(RankingPeriod period, LocalDate referenceDate) {
        return queryFactory
            .select(Projections.constructor(RankingStatsProjection.class,
                ranking.count(),
                ranking.profitRate.max(),
                ranking.profitRate.avg().castToNum(BigDecimal.class)))
            .from(ranking)
            .where(ranking.period.eq(period)
                .and(ranking.referenceDate.eq(referenceDate)))
            .fetchOne();
    }

    private BooleanExpression cursorRankGt(Integer cursorRank) {
        return cursorRank != null ? ranking.rank.gt(cursorRank) : null;
    }
}
