package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.ranking.application.port.in.GetMyRankingUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetMyRankingQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.MyRankingResult;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
import ksh.tryptobackend.ranking.application.port.out.dto.RankingWithUserProjection;
import ksh.tryptobackend.ranking.domain.vo.ProfitRate;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetMyRankingService implements GetMyRankingUseCase {

    private final RankingQueryPort rankingQueryPort;

    @Override
    @Transactional(readOnly = true)
    public MyRankingResult getMyRanking(GetMyRankingQuery query) {
        return findLatestReferenceDate(query.period())
            .flatMap(latestDate -> findMyRanking(query, latestDate))
            .orElse(null);
    }

    private Optional<LocalDate> findLatestReferenceDate(RankingPeriod period) {
        return rankingQueryPort.findLatestReferenceDate(period);
    }

    private Optional<MyRankingResult> findMyRanking(GetMyRankingQuery query, LocalDate latestDate) {
        return rankingQueryPort.findByUserIdAndPeriodAndReferenceDate(
                query.userId(), query.period(), latestDate)
            .map(this::toResult);
    }

    private MyRankingResult toResult(RankingWithUserProjection projection) {
        return new MyRankingResult(
            projection.rank(),
            projection.nickname(),
            ProfitRate.of(projection.profitRate()),
            projection.tradeCount()
        );
    }
}
