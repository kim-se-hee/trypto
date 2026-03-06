package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.ranking.application.port.in.GetRankerPortfolioUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankerPortfolioQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.PortfolioHoldingResult;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankerPortfolioResult;
import ksh.tryptobackend.ranking.application.port.out.ActiveRoundPort;
import ksh.tryptobackend.ranking.application.port.out.PortfolioSnapshotPort;
import ksh.tryptobackend.ranking.application.port.out.RankingPersistencePort;
import ksh.tryptobackend.ranking.application.port.out.dto.RankingWithUserProjection;
import ksh.tryptobackend.ranking.domain.model.Ranking;
import ksh.tryptobackend.ranking.domain.vo.ActiveRound;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetRankerPortfolioService implements GetRankerPortfolioUseCase {

    private final RankingPersistencePort rankingPersistencePort;
    private final ActiveRoundPort activeRoundPort;
    private final PortfolioSnapshotPort portfolioSnapshotPort;

    @Override
    @Transactional(readOnly = true)
    public RankerPortfolioResult getRankerPortfolio(GetRankerPortfolioQuery query) {
        LocalDate latestDate = findLatestReferenceDate(query);
        RankingWithUserProjection ranking = findRanking(query, latestDate);
        validateTop100(ranking);
        validatePortfolioPublic(ranking);
        ActiveRound round = findActiveRound(query.userId());
        List<PortfolioHoldingResult> holdings = findHoldings(query.userId(), round.roundId());
        return buildResult(ranking, holdings);
    }

    private LocalDate findLatestReferenceDate(GetRankerPortfolioQuery query) {
        return rankingPersistencePort.findLatestReferenceDate(query.period())
            .orElseThrow(() -> new CustomException(ErrorCode.RANKING_NOT_FOUND));
    }

    private RankingWithUserProjection findRanking(GetRankerPortfolioQuery query, LocalDate latestDate) {
        return rankingPersistencePort.findByUserIdAndPeriodAndReferenceDate(
                query.userId(), query.period(), latestDate)
            .orElseThrow(() -> new CustomException(ErrorCode.PORTFOLIO_VIEW_NOT_ALLOWED));
    }

    private void validateTop100(RankingWithUserProjection ranking) {
        if (!Ranking.isTop100(ranking.rank())) {
            throw new CustomException(ErrorCode.PORTFOLIO_VIEW_NOT_ALLOWED);
        }
    }

    private void validatePortfolioPublic(RankingWithUserProjection ranking) {
        if (!ranking.portfolioPublic()) {
            throw new CustomException(ErrorCode.PORTFOLIO_PRIVATE);
        }
    }

    private ActiveRound findActiveRound(Long userId) {
        return activeRoundPort.findActiveRoundByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_ACTIVE));
    }

    private List<PortfolioHoldingResult> findHoldings(Long userId, Long roundId) {
        return portfolioSnapshotPort.findLatestSnapshotDetails(userId, roundId).stream()
            .map(p -> new PortfolioHoldingResult(p.coinSymbol(), p.exchangeName(), p.assetRatio(), p.profitRate()))
            .toList();
    }

    private RankerPortfolioResult buildResult(RankingWithUserProjection ranking,
                                               List<PortfolioHoldingResult> holdings) {
        return new RankerPortfolioResult(
            ranking.userId(),
            ranking.nickname(),
            ranking.rank(),
            ranking.profitRate(),
            holdings
        );
    }
}
