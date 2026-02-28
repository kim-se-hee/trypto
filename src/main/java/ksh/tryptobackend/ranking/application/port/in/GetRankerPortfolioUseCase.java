package ksh.tryptobackend.ranking.application.port.in;

import ksh.tryptobackend.ranking.application.port.in.dto.query.GetRankerPortfolioQuery;
import ksh.tryptobackend.ranking.application.port.in.dto.result.RankerPortfolioResult;

public interface GetRankerPortfolioUseCase {

    RankerPortfolioResult getRankerPortfolio(GetRankerPortfolioQuery query);
}
