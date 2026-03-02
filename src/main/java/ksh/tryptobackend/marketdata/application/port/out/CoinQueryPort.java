package ksh.tryptobackend.marketdata.application.port.out;

import java.util.Map;
import java.util.Set;

public interface CoinQueryPort {

    Map<Long, String> findSymbolsByIds(Set<Long> coinIds);

    Map<Long, String> findSymbolsByExchangeCoinIds(Set<Long> exchangeCoinIds);

    Map<Long, Long> findCoinIdsByExchangeCoinIds(Set<Long> exchangeCoinIds);
}
