package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.ResolveExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ResolveExchangeCoinMappingService implements ResolveExchangeCoinMappingUseCase {

    private final ExchangeCoinMappingCacheQueryPort exchangeCoinMappingCacheQueryPort;

    @Override
    public Optional<Long> resolve(String exchange, String symbol) {
        return exchangeCoinMappingCacheQueryPort.resolve(exchange, symbol)
            .map(mapping -> mapping.exchangeCoinId());
    }
}
