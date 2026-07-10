package ksh.tryptobackend.ranking.adapter.out.acl;

import java.util.Set;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinSymbolsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeNamesUseCase;
import ksh.tryptobackend.ranking.application.port.out.MarketDataQueryPort;
import ksh.tryptobackend.ranking.domain.vo.CoinSymbols;
import ksh.tryptobackend.ranking.domain.vo.ExchangeNames;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RankingAclMarketDataQueryAdapter implements MarketDataQueryPort {

    private final FindCoinSymbolsUseCase findCoinSymbolsUseCase;
    private final FindExchangeNamesUseCase findExchangeNamesUseCase;

    @Override
    public CoinSymbols findCoinSymbols(Set<Long> coinIds) {
        return new CoinSymbols(findCoinSymbolsUseCase.findSymbolsByIds(coinIds));
    }

    @Override
    public ExchangeNames findExchangeNames(Set<Long> exchangeIds) {
        return new ExchangeNames(findExchangeNamesUseCase.findExchangeNames(exchangeIds));
    }
}
