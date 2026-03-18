package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.BroadcastLiveTickerUseCase;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.LivePriceMessagePort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.LiveTicker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class BroadcastLiveTickerService implements BroadcastLiveTickerUseCase {

    private final ExchangeCoinMappingCacheQueryPort exchangeCoinMappingCacheQueryPort;
    private final LivePriceMessagePort livePriceMessagePort;

    @Override
    public void broadcast(String exchange, String symbol, BigDecimal currentPrice,
                          BigDecimal changeRate, BigDecimal quoteTurnover, Long timestamp) {
        ExchangeCoinMapping mapping = exchangeCoinMappingCacheQueryPort.resolve(exchange, symbol)
            .orElse(null);
        if (mapping == null) {
            return;
        }

        LiveTicker liveTicker = new LiveTicker(
            mapping.coinId(), mapping.coinSymbol(), currentPrice,
            changeRate, quoteTurnover, timestamp);
        livePriceMessagePort.send(mapping.exchangeId(), liveTicker);
    }
}
