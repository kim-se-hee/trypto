package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.vo.TradingVenue;

import java.util.Optional;

public interface TradingVenuePort {

    Optional<TradingVenue> findByExchangeId(Long exchangeId);
}
