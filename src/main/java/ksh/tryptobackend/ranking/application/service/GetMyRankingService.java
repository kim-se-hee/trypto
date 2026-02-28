package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.ranking.application.port.in.GetMyRankingUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetMyRankingQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.MyRankingResult;
import ksh.tryptobackend.ranking.application.port.out.RankingPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class GetMyRankingService implements GetMyRankingUseCase {

    private final RankingPersistencePort rankingPersistencePort;

    @Override
    @Transactional(readOnly = true)
    public MyRankingResult getMyRanking(GetMyRankingQuery query) {
        LocalDate latestDate = findLatestReferenceDate(query);
        if (latestDate == null) {
            return null;
        }
        return findMyRanking(query, latestDate);
    }

    private LocalDate findLatestReferenceDate(GetMyRankingQuery query) {
        return rankingPersistencePort.findLatestReferenceDate(query.period())
            .orElse(null);
    }

    private MyRankingResult findMyRanking(GetMyRankingQuery query, LocalDate latestDate) {
        return rankingPersistencePort.findByUserIdAndPeriodAndReferenceDate(
                query.userId(), query.period(), latestDate)
            .map(MyRankingResult::from)
            .orElse(null);
    }
}
