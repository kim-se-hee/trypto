package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;
import ksh.tryptobackend.marketdata.domain.vo.Tick;

import java.time.Instant;
import java.util.List;

public interface TickQueryPort {

    List<Tick> findTicks(ExchangeSymbolKey key, Instant from, Instant to);
}
