package ksh.tryptobackend.ranking.application.port.out;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import ksh.tryptobackend.ranking.domain.vo.RankingSummary;

public interface RankingQueryPort {

    Optional<LocalDate> findLatestReferenceDate(RankingPeriod period);

    List<RankingSummary> findRankings(
            RankingPeriod period, LocalDate referenceDate, Integer cursorRank, int size);

    List<RankingSummary> findAllRankings(RankingPeriod period, LocalDate referenceDate);

    Optional<RankingSummary> findByUserIdAndPeriodAndReferenceDate(
            Long userId, RankingPeriod period, LocalDate referenceDate);
}
