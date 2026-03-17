package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.vo.ExchangeSymbolKey;

import java.util.Map;
import java.util.Optional;

public interface ExchangeCoinMappingCachePort {

    Optional<Long> resolve(String exchange, String symbol);

    void loadAll(Map<ExchangeSymbolKey, Long> mappings);
}
