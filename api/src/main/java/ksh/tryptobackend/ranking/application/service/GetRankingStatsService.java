package ksh.tryptobackend.ranking.application.service;

import java.time.LocalDate;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.ranking.application.port.in.GetRankingStatsUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingStatsQuery;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
import ksh.tryptobackend.ranking.domain.vo.RankingStats;
import ksh.tryptobackend.ranking.domain.vo.RankingSummaries;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRankingStatsService implements GetRankingStatsUseCase {

    private final RankingQueryPort rankingQueryPort;

    @Override
    @Transactional(readOnly = true)
    public RankingStats getRankingStats(GetRankingStatsQuery query) {
        LocalDate referenceDate =
                rankingQueryPort
                        .findLatestReferenceDate(query.period())
                        .orElseThrow(() -> new CustomException(ErrorCode.RANKING_NOT_FOUND));
        return RankingSummaries.of(rankingQueryPort.findAllRankings(query.period(), referenceDate))
                .toStats();
    }
}
