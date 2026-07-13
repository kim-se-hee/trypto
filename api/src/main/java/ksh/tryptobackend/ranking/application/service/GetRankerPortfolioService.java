package ksh.tryptobackend.ranking.application.service;

import java.time.LocalDate;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.ranking.application.port.in.GetRankerPortfolioUseCase;
import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankerPortfolioQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankerPortfolioResult;
import ksh.tryptobackend.ranking.application.port.out.InvestmentRoundQueryPort;
import ksh.tryptobackend.ranking.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.ranking.application.port.out.PortfolioQueryPort;
import ksh.tryptobackend.ranking.application.port.out.RankingQueryPort;
import ksh.tryptobackend.ranking.application.port.out.UserQueryPort;
import ksh.tryptobackend.ranking.domain.vo.CoinSymbols;
import ksh.tryptobackend.ranking.domain.vo.ExchangeNames;
import ksh.tryptobackend.ranking.domain.vo.Holdings;
import ksh.tryptobackend.ranking.domain.vo.RankingSummary;
import ksh.tryptobackend.ranking.domain.vo.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetRankerPortfolioService implements GetRankerPortfolioUseCase {

    private final RankingQueryPort rankingQueryPort;
    private final UserQueryPort userQueryPort;
    private final PortfolioQueryPort portfolioQueryPort;
    private final MarketDataQueryPort marketDataQueryPort;
    private final InvestmentRoundQueryPort investmentRoundQueryPort;

    @Override
    @Transactional(readOnly = true)
    public RankerPortfolioResult getRankerPortfolio(GetRankerPortfolioQuery query) {
        LocalDate referenceDate = rankingQueryPort
                .findLatestReferenceDate(query.period())
                .orElseThrow(() -> new CustomException(ErrorCode.RANKING_NOT_FOUND));
        RankingSummary ranking = rankingQueryPort
                .findByUserIdAndPeriodAndReferenceDate(query.userId(), query.period(), referenceDate)
                .orElseThrow(() -> new CustomException(ErrorCode.PORTFOLIO_VIEW_NOT_ALLOWED));
        ranking.assertViewable();
        UserProfile viewer = userQueryPort.getByUserId(query.userId());
        Long roundId = investmentRoundQueryPort.getActiveRoundId(query.userId());
        Holdings holdings = portfolioQueryPort.findLatestHoldings(query.userId(), roundId);
        CoinSymbols coinSymbols = marketDataQueryPort.findCoinSymbols(holdings.coinIds());
        ExchangeNames exchangeNames = marketDataQueryPort.findExchangeNames(holdings.exchangeIds());

        return RankerPortfolioResult.of(ranking, viewer, holdings.describe(coinSymbols, exchangeNames));
    }
}
