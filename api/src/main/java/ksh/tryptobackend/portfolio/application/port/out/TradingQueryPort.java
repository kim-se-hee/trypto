package ksh.tryptobackend.portfolio.application.port.out;

import ksh.tryptobackend.portfolio.domain.model.EvaluatedHoldings;
import ksh.tryptobackend.portfolio.domain.vo.PortfolioHoldings;

public interface TradingQueryPort {

    PortfolioHoldings findHoldings(Long walletId, Long exchangeId);

    EvaluatedHoldings findEvaluatedHoldings(Long walletId, Long exchangeId);
}
