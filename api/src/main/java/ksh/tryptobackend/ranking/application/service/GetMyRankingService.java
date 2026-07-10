package ksh.tryptobackend.ranking.application.service;

import java.util.Set;
import ksh.tryptobackend.ranking.application.port.in.GetMyRankingUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetMyRankingQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.MyRankingResult;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
import ksh.tryptobackend.ranking.application.port.out.UserQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMyRankingService implements GetMyRankingUseCase {

    private final RankingQueryPort rankingQueryPort;
    private final UserQueryPort userQueryPort;

    @Override
    @Transactional(readOnly = true)
    public MyRankingResult getMyRanking(GetMyRankingQuery query) {
        return rankingQueryPort
                .findLatestReferenceDate(query.period())
                .flatMap(
                        referenceDate ->
                                rankingQueryPort.findByUserIdAndPeriodAndReferenceDate(
                                        query.userId(), query.period(), referenceDate))
                .map(
                        summary ->
                                MyRankingResult.of(
                                        summary,
                                        userQueryPort.findByUserIds(Set.of(summary.userId()))))
                .orElse(null);
    }
}
