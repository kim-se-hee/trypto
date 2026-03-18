package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheCommandPort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExchangeCoinMappingCacheCommandAdapter implements ExchangeCoinMappingCacheCommandPort {

    private final ConcurrentHashMap<ExchangeSymbolKey, ExchangeCoinMapping> cache = new ConcurrentHashMap<>();

    @Override
    public void loadAll(Map<ExchangeSymbolKey, ExchangeCoinMapping> mappings) {
        cache.clear();
        cache.putAll(mappings);
    }

    Optional<ExchangeCoinMapping> resolve(String exchange, String symbol) {
        return Optional.ofNullable(cache.get(new ExchangeSymbolKey(exchange, symbol)));
    }
}
