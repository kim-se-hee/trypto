package ksh.tryptobackend.ranking.application.port.out;

import ksh.tryptobackend.ranking.application.port.out.dto.RankingStatsProjection;
import ksh.tryptobackend.ranking.application.port.out.dto.RankingWithUserProjection;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.Optional;

public interface RankingPersistencePort {

    Optional<LocalDate> findLatestReferenceDate(RankingPeriod period);

    Page<RankingWithUserProjection> findRankings(RankingPeriod period, LocalDate referenceDate, Pageable pageable);

    Optional<RankingWithUserProjection> findByUserIdAndPeriodAndReferenceDate(Long userId, RankingPeriod period, LocalDate referenceDate);

    RankingStatsProjection getRankingStats(RankingPeriod period, LocalDate referenceDate);
}
