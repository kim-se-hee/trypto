package ksh.tryptobackend.trading.application.port.out;

import java.util.Optional;

public interface ExchangeCoinMappingCacheQueryPort {

    Optional<Long> resolve(String exchange, String symbol);
}
