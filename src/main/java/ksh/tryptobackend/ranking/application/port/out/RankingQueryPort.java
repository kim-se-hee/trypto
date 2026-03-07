package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.application.port.out.dto.RankingStatsProjection;
import ksh.tryptobackend.ranking.application.port.out.dto.RankingWithUserProjection;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RankingQueryPort {

    Optional<LocalDate> findLatestReferenceDate(RankingPeriod period);

    List<RankingWithUserProjection> findRankings(RankingPeriod period, LocalDate referenceDate, Integer cursorRank, int size);

    Optional<RankingWithUserProjection> findByUserIdAndPeriodAndReferenceDate(Long userId, RankingPeriod period, LocalDate referenceDate);

    RankingStatsProjection getRankingStats(RankingPeriod period, LocalDate referenceDate);
}
