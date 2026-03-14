package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.ranking.application.port.in.GetMyRankingUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetMyRankingQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.MyRankingResult;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
import ksh.tryptobackend.ranking.domain.vo.RankingSummary;
import ksh.tryptobackend.common.domain.vo.ProfitRate;
import ksh.tryptobackend.ranking.domain.vo.RankingPeriod;
import ksh.tryptobackend.user.application.port.in.FindUserPublicInfoUseCase;
import ksh.tryptobackend.user.application.port.in.dto.result.UserPublicInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GetMyRankingService implements GetMyRankingUseCase {

    private final RankingQueryPort rankingQueryPort;
    private final FindUserPublicInfoUseCase findUserPublicInfoUseCase;

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
            .map(ranking -> toResult(ranking, query.userId()));
    }

    private MyRankingResult toResult(RankingSummary ranking, Long userId) {
        String nickname = findUserPublicInfoUseCase.findByUserId(userId)
            .map(UserPublicInfoResult::nickname)
            .orElse("");
        return new MyRankingResult(
            ranking.rank(),
            nickname,
            ProfitRate.of(ranking.profitRate()),
            ranking.tradeCount()
        );
    }
}
