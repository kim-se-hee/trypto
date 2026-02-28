package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.ranking.application.port.in.GetRankingStatsUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingStatsQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingStatsResult;
import ksh.tryptobackend.ranking.application.port.out.RankingPersistencePort;
import ksh.tryptobackend.ranking.application.port.out.dto.RankingStatsProjection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetRankingStatsService implements GetRankingStatsUseCase {

    private final RankingPersistencePort rankingPersistencePort;

    @Override
    @Transactional(readOnly = true)
    public RankingStatsResult getRankingStats(GetRankingStatsQuery query) {
        LocalDate latestDate = findLatestReferenceDate(query);
        return buildStats(query, latestDate);
    }

    private LocalDate findLatestReferenceDate(GetRankingStatsQuery query) {
        return rankingPersistencePort.findLatestReferenceDate(query.period())
            .orElseThrow(() -> new CustomException(ErrorCode.RANKING_NOT_FOUND));
    }

    private RankingStatsResult buildStats(GetRankingStatsQuery query, LocalDate latestDate) {
        RankingStatsProjection stats = rankingPersistencePort.getRankingStats(query.period(), latestDate);
        return new RankingStatsResult(
            stats.totalParticipants(),
            stats.maxProfitRate(),
            stats.avgProfitRate()
        );
    }
}
