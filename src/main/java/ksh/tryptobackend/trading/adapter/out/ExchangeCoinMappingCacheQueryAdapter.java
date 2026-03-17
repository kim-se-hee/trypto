package ksh.tryptobackend.trading.adapter.out;

import ksh.tryptobackend.trading.application.port.out.ExchangeCoinMappingCacheQueryPort;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ExchangeCoinMappingCacheQueryAdapter implements ExchangeCoinMappingCacheQueryPort {

    private final ExchangeCoinMappingCacheCommandAdapter cacheStore;

    public ExchangeCoinMappingCacheQueryAdapter(ExchangeCoinMappingCacheCommandAdapter cacheStore) {
        this.cacheStore = cacheStore;
    }

    @Override
    public Optional<Long> resolve(String exchange, String symbol) {
        return cacheStore.resolve(exchange, symbol);
    }
}
