package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.ResolveLiveTickerUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerResult;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheQueryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResolveLiveTickerService implements ResolveLiveTickerUseCase {

    private final ExchangeCoinMappingCacheQueryPort exchangeCoinMappingCacheQueryPort;

    @Override
    public Optional<LiveTickerResult> resolve(String exchange, String symbol, BigDecimal currentPrice,
                                              BigDecimal changeRate, BigDecimal quoteTurnover, Long timestamp) {
        return exchangeCoinMappingCacheQueryPort.resolve(exchange, symbol)
            .map(mapping -> new LiveTickerResult(
                mapping.exchangeId(), mapping.coinId(), mapping.coinSymbol(),
                currentPrice, changeRate, quoteTurnover, timestamp));
    }
}
