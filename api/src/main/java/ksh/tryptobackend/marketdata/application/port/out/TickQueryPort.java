package ksh.tryptobackend.marketdata.application.port.out;

import java.time.Instant;
import java.util.List;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import ksh.tryptobackend.marketdata.domain.vo.Tick;

public interface TickQueryPort {

    List<Tick> findTicks(ExchangeSymbolKey key, Instant from, Instant to);
}
