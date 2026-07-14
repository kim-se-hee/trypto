package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.ranking.application.port.in.GetRankingStatsUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankingStatsQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankingStatsResult;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
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
        // 집계된 랭킹이 하나도 없으면 기준 날짜를 정할 수 없다. 배치가 아직 돌지 않은 정상 상태이므로 빈 통계로 응답한다.
        return rankingQueryPort
                .findLatestReferenceDate(query.period())
                .map(referenceDate -> rankingQueryPort.getRankingStats(query.period(), referenceDate))
                .map(stats ->
                        new RankingStatsResult(stats.totalParticipants(), stats.maxProfitRate(), stats.avgProfitRate()))
                .orElseGet(RankingStatsResult::empty);
    }
}
