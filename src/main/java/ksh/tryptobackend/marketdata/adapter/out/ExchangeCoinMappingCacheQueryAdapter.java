package ksh.tryptobackend.marketdata.adapter.out;

import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ExchangeCoinMappingCacheQueryAdapter implements ExchangeCoinMappingCacheQueryPort {

    private final ExchangeCoinMappingCacheCommandAdapter cacheStore;

    public ExchangeCoinMappingCacheQueryAdapter(ExchangeCoinMappingCacheCommandAdapter cacheStore) {
        this.cacheStore = cacheStore;
    }

    @Override
    public Optional<ExchangeCoinMapping> resolve(String exchange, String symbol) {
        return cacheStore.resolve(exchange, symbol);
    }
}
