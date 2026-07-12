package ksh.tryptobackend.marketdata.application.service;

import java.time.Instant;
import java.util.List;
import ksh.tryptobackend.marketdata.application.port.in.FindTicksUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.TickResult;
import ksh.tryptobackend.marketdata.application.port.out.TickQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindTicksService implements FindTicksUseCase {

    private final TickQueryPort tickQueryPort;

    @Override
    public List<TickResult> findTicks(String exchangeName, String marketSymbol, Instant from, Instant to) {
        return tickQueryPort.findTicks(new ExchangeSymbolKey(exchangeName, marketSymbol), from, to).stream()
                .map(t -> new TickResult(t.time(), t.price()))
                .toList();
    }
}
