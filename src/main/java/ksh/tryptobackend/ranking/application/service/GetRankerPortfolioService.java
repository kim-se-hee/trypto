package ksh.tryptobackend.ranking.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.investmentround.application.port.in.FindRoundInfoUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinSymbolsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeNamesUseCase;
import ksh.tryptobackend.portfolio.application.port.in.FindSnapshotDetailsUseCase;
import ksh.tryptobackend.portfolio.application.port.in.dto.result.SnapshotDetailResult;
import ksh.tryptobackend.ranking.application.port.in.GetRankerPortfolioUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankerPortfolioQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.PortfolioHoldingResult;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankerPortfolioResult;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
import ksh.tryptobackend.ranking.domain.vo.RankingSummary;
import ksh.tryptobackend.ranking.domain.model.Ranking;
import ksh.tryptobackend.ranking.domain.vo.CoinSymbols;
import ksh.tryptobackend.ranking.domain.vo.ExchangeNames;
import ksh.tryptobackend.user.application.port.in.FindUserPublicInfoUseCase;
import ksh.tryptobackend.user.application.port.in.dto.result.UserPublicInfoResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetRankerPortfolioService implements GetRankerPortfolioUseCase {

    private final RankingQueryPort rankingQueryPort;
    private final FindRoundInfoUseCase findRoundInfoUseCase;
    private final FindSnapshotDetailsUseCase findSnapshotDetailsUseCase;
    private final FindCoinSymbolsUseCase findCoinSymbolsUseCase;
    private final FindExchangeNamesUseCase findExchangeNamesUseCase;
    private final FindUserPublicInfoUseCase findUserPublicInfoUseCase;

    @Override
    @Transactional(readOnly = true)
    public RankerPortfolioResult getRankerPortfolio(GetRankerPortfolioQuery query) {
        LocalDate latestDate = findLatestReferenceDate(query);
        RankingSummary ranking = findRanking(query, latestDate);
        validateTop100(ranking);
        UserPublicInfoResult userInfo = getUserInfo(query.userId());
        validatePortfolioPublic(userInfo);
        Long roundId = findActiveRoundId(query.userId());
        List<PortfolioHoldingResult> holdings = findHoldings(query.userId(), roundId);
        return buildResult(ranking, userInfo, holdings);
    }

    private LocalDate findLatestReferenceDate(GetRankerPortfolioQuery query) {
        return rankingQueryPort.findLatestReferenceDate(query.period())
            .orElseThrow(() -> new CustomException(ErrorCode.RANKING_NOT_FOUND));
    }

    private RankingSummary findRanking(GetRankerPortfolioQuery query, LocalDate latestDate) {
        return rankingQueryPort.findByUserIdAndPeriodAndReferenceDate(
                query.userId(), query.period(), latestDate)
            .orElseThrow(() -> new CustomException(ErrorCode.PORTFOLIO_VIEW_NOT_ALLOWED));
    }

    private void validateTop100(RankingSummary ranking) {
        if (!Ranking.isTop100(ranking.rank())) {
            throw new CustomException(ErrorCode.PORTFOLIO_VIEW_NOT_ALLOWED);
        }
    }

    private UserPublicInfoResult getUserInfo(Long userId) {
        return findUserPublicInfoUseCase.findByUserId(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private void validatePortfolioPublic(UserPublicInfoResult userInfo) {
        if (!userInfo.portfolioPublic()) {
            throw new CustomException(ErrorCode.PORTFOLIO_PRIVATE);
        }
    }

    private Long findActiveRoundId(Long userId) {
        return findRoundInfoUseCase.findActiveByUserId(userId)
            .map(result -> result.roundId())
            .orElseThrow(() -> new CustomException(ErrorCode.ROUND_NOT_ACTIVE));
    }

    private List<PortfolioHoldingResult> findHoldings(Long userId, Long roundId) {
        List<SnapshotDetailResult> details = findSnapshotDetailsUseCase.findLatestSnapshotDetails(userId, roundId);

        CoinSymbols coinSymbols = resolveCoinSymbols(details);
        ExchangeNames exchangeNames = resolveExchangeNames(details);

        return details.stream()
            .map(detail -> toHoldingResult(detail, coinSymbols, exchangeNames))
            .toList();
    }

    private CoinSymbols resolveCoinSymbols(List<SnapshotDetailResult> details) {
        Set<Long> coinIds = details.stream()
            .map(SnapshotDetailResult::coinId)
            .collect(Collectors.toSet());
        return new CoinSymbols(findCoinSymbolsUseCase.findSymbolsByIds(coinIds));
    }

    private ExchangeNames resolveExchangeNames(List<SnapshotDetailResult> details) {
        Set<Long> exchangeIds = details.stream()
            .map(SnapshotDetailResult::exchangeId)
            .collect(Collectors.toSet());
        return new ExchangeNames(findExchangeNamesUseCase.findExchangeNames(exchangeIds));
    }

    private PortfolioHoldingResult toHoldingResult(SnapshotDetailResult detail,
                                                    CoinSymbols coinSymbols,
                                                    ExchangeNames exchangeNames) {
        return new PortfolioHoldingResult(
            coinSymbols.getSymbol(detail.coinId()),
            exchangeNames.getName(detail.exchangeId()),
            detail.assetRatio(),
            detail.profitRate()
        );
    }

    private RankerPortfolioResult buildResult(RankingSummary ranking,
                                               UserPublicInfoResult userInfo,
                                               List<PortfolioHoldingResult> holdings) {
        return new RankerPortfolioResult(
            ranking.userId(),
            userInfo.nickname(),
            ranking.rank(),
            ranking.profitRate(),
            holdings
        );
    }
}
