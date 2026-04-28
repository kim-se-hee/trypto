package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindTicksUseCase;
import ksh.tryptobackend.marketdata.application.port.out.TickQueryPort;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import ksh.tryptobackend.marketdata.domain.vo.Tick;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FindTicksService implements FindTicksUseCase {

    private final TickQueryPort tickQueryPort;

    @Override
    public List<Tick> findTicks(ExchangeSymbolKey key, Instant from, Instant to) {
        return tickQueryPort.findTicks(key, from, to);
    }
}
