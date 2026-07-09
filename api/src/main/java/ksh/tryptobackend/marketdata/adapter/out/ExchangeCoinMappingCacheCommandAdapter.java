package ksh.tryptobackend.marketdata.adapter.out;

import java.util.Map;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheCommandPort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExchangeCoinMappingCacheCommandAdapter implements ExchangeCoinMappingCacheCommandPort {

    private final ExchangeCoinMappingCacheStore store;

    @Override
    public void loadAll(Map<ExchangeSymbolKey, ExchangeCoinMapping> mappings) {
        store.loadAll(mappings);
    }
}
