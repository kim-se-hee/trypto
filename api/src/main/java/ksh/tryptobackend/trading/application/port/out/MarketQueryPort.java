package ksh.tryptobackend.trading.application.port.out;

import java.time.Instant;
import java.util.List;
import ksh.tryptobackend.trading.domain.vo.CoinExchangeMapping;
import ksh.tryptobackend.trading.domain.vo.MarketIdentifier;
import ksh.tryptobackend.trading.domain.vo.MarketInfo;
import ksh.tryptobackend.trading.domain.vo.Price;
import ksh.tryptobackend.trading.domain.vo.PriceCandidates;
import ksh.tryptobackend.trading.domain.vo.TradingPair;

public interface MarketQueryPort {

    MarketInfo findByExchangeCoinId(Long exchangeCoinId);

    TradingPair getTradingPair(Long exchangeCoinId);

    MarketIdentifier findMarketIdentifier(Long exchangeCoinId);

    Price getCurrentPrice(Long exchangeCoinId);

    CoinExchangeMapping findCoinExchangeMapping(Long exchangeId, List<Long> coinIds);

    PriceCandidates findPriceCandidates(MarketIdentifier market, Instant from, Instant to);
}
