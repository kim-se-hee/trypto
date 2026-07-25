package ksh.tryptobackend.trading.application.service;

import java.util.List;
import ksh.tryptobackend.trading.application.port.in.FindSuspendedMarketsUseCase;
import ksh.tryptobackend.trading.application.port.out.MarketQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindSuspendedMarketsService implements FindSuspendedMarketsUseCase {

    private final MarketQueryPort marketQueryPort;

    @Override
    public List<Long> findSuspendedMarkets() {
        return marketQueryPort.findSuspendedExchangeCoinIds();
    }
}
