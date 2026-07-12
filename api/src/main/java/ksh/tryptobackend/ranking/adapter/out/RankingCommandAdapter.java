package ksh.tryptobackend.ranking.adapter.out;

import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import ksh.tryptobackend.ranking.adapter.out.persistence.entity.QRankingJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.persistence.entity.RankingJpaEntity;
import ksh.tryptobackend.ranking.adapter.out.persistence.repository.RankingJpaRepository;
import ksh.tryptobackend.ranking.application.port.out.RankingCommandPort;
import ksh.tryptobackend.ranking.domain.model.Ranking;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class RankingCommandAdapter implements RankingCommandPort {

    private final RankingJpaRepository rankingJpaRepository;
    private final JPAQueryFactory queryFactory;

    private static final QRankingJpaEntity ranking = QRankingJpaEntity.rankingJpaEntity;

    @Override
    @Transactional
    public void replaceByPeriodAndDate(List<Ranking> rankings, RankingPeriod period, LocalDate referenceDate) {
        queryFactory
                .delete(ranking)
                .where(ranking.period.eq(period).and(ranking.referenceDate.eq(referenceDate)))
                .execute();
        List<RankingJpaEntity> entities =
                rankings.stream().map(RankingJpaEntity::fromDomain).toList();
        rankingJpaRepository.saveAll(entities);
    }
}
