package ksh.tryptobackend.portfolio.application.port.out;

import java.util.Set;
import ksh.tryptobackend.portfolio.domain.vo.CoinMetadataMap;

public interface MarketDataQueryPort {

    Long getBaseCurrencyCoinId(Long exchangeId);

    CoinMetadataMap findCoinMetadata(Set<Long> coinIds);
}
