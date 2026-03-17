package ksh.tryptobackend.trading.adapter.out;

import ksh.tryptobackend.trading.application.port.out.ExchangeCoinMappingCacheCommandPort;
import ksh.tryptobackend.trading.domain.vo.ExchangeSymbolKey;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ExchangeCoinMappingCacheCommandAdapter implements ExchangeCoinMappingCacheCommandPort {

    private final ConcurrentHashMap<ExchangeSymbolKey, Long> cache = new ConcurrentHashMap<>();

    @Override
    public void loadAll(Map<ExchangeSymbolKey, Long> mappings) {
        cache.clear();
        cache.putAll(mappings);
    }

    Optional<Long> resolve(String exchange, String symbol) {
        return Optional.ofNullable(cache.get(new ExchangeSymbolKey(exchange, symbol)));
    }
}
