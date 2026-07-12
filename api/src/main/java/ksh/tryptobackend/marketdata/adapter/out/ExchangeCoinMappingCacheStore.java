package ksh.tryptobackend.marketdata.adapter.out;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import org.springframework.stereotype.Component;

@Component
public class ExchangeCoinMappingCacheStore {

    private final ConcurrentHashMap<ExchangeSymbolKey, ExchangeCoinMapping> cache = new ConcurrentHashMap<>();

    public void loadAll(Map<ExchangeSymbolKey, ExchangeCoinMapping> mappings) {
        cache.clear();
        cache.putAll(mappings);
    }

    public Optional<ExchangeCoinMapping> resolve(String exchange, String symbol) {
        return Optional.ofNullable(cache.get(new ExchangeSymbolKey(exchange, symbol)));
    }
}
