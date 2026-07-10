package ksh.tryptobackend.portfolio.application.port.out;

import java.util.Set;
import ksh.tryptobackend.portfolio.domain.vo.CoinMetadataMap;
import ksh.tryptobackend.portfolio.domain.vo.ExchangeSnapshot;

public interface MarketDataQueryPort {

    Long getBaseCurrencyCoinId(Long exchangeId);

    ExchangeSnapshot getExchangeSnapshot(Long exchangeId);

    CoinMetadataMap findCoinMetadata(Set<Long> coinIds);
}
