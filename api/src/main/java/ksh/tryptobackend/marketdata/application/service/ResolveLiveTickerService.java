package ksh.tryptobackend.marketdata.application.service;

import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.in.ResolveLiveTickerUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.command.ResolveLiveTickerCommand;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerBatchResult;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.LiveTickerResult;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinMappingCacheQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ResolveLiveTickerService implements ResolveLiveTickerUseCase {

    private final ExchangeCoinMappingCacheQueryPort exchangeCoinMappingCacheQueryPort;

    @Override
    public Optional<LiveTickerBatchResult> resolve(ResolveLiveTickerCommand command) {
        return LiveTickerBatchResult.from(command.tickers().stream()
                .map(ticker -> exchangeCoinMappingCacheQueryPort
                        .resolve(command.exchange(), ticker.symbol())
                        .map(mapping -> LiveTickerResult.of(mapping, ticker)))
                .flatMap(Optional::stream)
                .toList());
    }
}
