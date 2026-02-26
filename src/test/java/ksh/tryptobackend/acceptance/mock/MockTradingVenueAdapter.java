package ksh.tryptobackend.acceptance.mock;

import ksh.tryptobackend.trading.application.port.out.TradingVenuePort;
import ksh.tryptobackend.trading.domain.vo.TradingVenue;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class MockTradingVenueAdapter implements TradingVenuePort {

    private final Map<Long, TradingVenue> venues = new ConcurrentHashMap<>();

    @Override
    public Optional<TradingVenue> findByExchangeId(Long exchangeId) {
        return Optional.ofNullable(venues.get(exchangeId));
    }

    public void addVenue(Long exchangeId, TradingVenue venue) {
        venues.put(exchangeId, venue);
    }

    public void clear() {
        venues.clear();
    }
}
