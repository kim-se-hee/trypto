package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.vo.MarketIdentifier;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import ksh.tryptobackend.trading.domain.vo.TradingPair;

public interface MarketQueryPort {

    MarketInfo findByExchangeCoinId(Long exchangeCoinId);

    TradingPair getTradingPair(Long exchangeCoinId);

    MarketIdentifier findMarketIdentifier(Long exchangeCoinId);
}
