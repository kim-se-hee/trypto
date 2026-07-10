package ksh.tryptobackend.ranking.application.port.out;

import java.util.Set;
import ksh.tryptobackend.ranking.domain.vo.CoinSymbols;
import ksh.tryptobackend.ranking.domain.vo.ExchangeNames;

public interface MarketDataQueryPort {

    CoinSymbols findCoinSymbols(Set<Long> coinIds);

    ExchangeNames findExchangeNames(Set<Long> exchangeIds);
}
