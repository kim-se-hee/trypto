package ksh.tryptobackend.ranking.application.service;

import java.time.LocalDate;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.ranking.application.port.in.GetRankingStatsUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingStatsQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingStatsResult;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
import ksh.tryptobackend.ranking.domain.vo.RankingStats;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRankingStatsService implements GetRankingStatsUseCase {

    private final RankingQueryPort rankingQueryPort;

    @Override
    @Transactional(readOnly = true)
    public RankingStatsResult getRankingStats(GetRankingStatsQuery query) {
        LocalDate referenceDate =
                rankingQueryPort
                        .findLatestReferenceDate(query.period())
                        .orElseThrow(() -> new CustomException(ErrorCode.RANKING_NOT_FOUND));
        RankingStats stats = rankingQueryPort.getRankingStats(query.period(), referenceDate);
        return new RankingStatsResult(
                stats.totalParticipants(), stats.maxProfitRate(), stats.avgProfitRate());
    }
}
