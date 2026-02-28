package ksh.tryptobackend.ranking.adapter.out;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import ksh.tryptobackend.ranking.adapter.out.entity.QRankingJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.entity.QRankingUserJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.repository.RankingJpaRepository;
import ksh.tryptobackend.ranking.application.port.out.RankingPersistencePort;
import ksh.tryptobackend.ranking.application.port.out.dto.RankingStatsProjection;
import ksh.tryptobackend.ranking.application.port.out.dto.RankingWithUserProjection;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RankingJpaPersistenceAdapter implements RankingPersistencePort {

    private final RankingJpaRepository rankingJpaRepository;
    private final JPAQueryFactory queryFactory;

    private static final QRankingJpaEntity ranking = QRankingJpaEntity.rankingJpaEntity;
    private static final QRankingUserJpaEntity user = QRankingUserJpaEntity.rankingUserJpaEntity;

    @Override
    public Optional<LocalDate> findLatestReferenceDate(RankingPeriod period) {
        return rankingJpaRepository.findLatestReferenceDate(period);
    }

    @Override
    public Page<RankingWithUserProjection> findRankings(RankingPeriod period, LocalDate referenceDate, Pageable pageable) {
        List<RankingWithUserProjection> content = queryFactory
            .select(Projections.constructor(RankingWithUserProjection.class,
                ranking.rank,
                ranking.userId,
                user.nickname,
                ranking.profitRate,
                ranking.tradeCount,
                user.portfolioPublic))
            .from(ranking)
            .join(user).on(ranking.userId.eq(user.id))
            .where(ranking.period.eq(period)
                .and(ranking.referenceDate.eq(referenceDate)))
            .orderBy(ranking.rank.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        long total = countRankings(period, referenceDate);

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public Optional<RankingWithUserProjection> findByUserIdAndPeriodAndReferenceDate(Long userId, RankingPeriod period, LocalDate referenceDate) {
        RankingWithUserProjection result = queryFactory
            .select(Projections.constructor(RankingWithUserProjection.class,
                ranking.rank,
                ranking.userId,
                user.nickname,
                ranking.profitRate,
                ranking.tradeCount,
                user.portfolioPublic))
            .from(ranking)
            .join(user).on(ranking.userId.eq(user.id))
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

    private long countRankings(RankingPeriod period, LocalDate referenceDate) {
        JPAQuery<Long> countQuery = queryFactory
            .select(ranking.count())
            .from(ranking)
            .where(ranking.period.eq(period)
                .and(ranking.referenceDate.eq(referenceDate)));

        Long count = countQuery.fetchOne();
        return count != null ? count : 0L;
    }
}
