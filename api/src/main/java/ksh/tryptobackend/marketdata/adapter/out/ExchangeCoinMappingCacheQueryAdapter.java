package ksh.tryptobackend.marketdata.adapter.out;

import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeCoinMappingCacheQueryAdapter implements ExchangeCoinMappingCacheQueryPort {

    private final ExchangeCoinMappingCacheStore store;

    @Override
    public Optional<ExchangeCoinMapping> resolve(String exchange, String symbol) {
        return store.resolve(exchange, symbol);
    }
}
