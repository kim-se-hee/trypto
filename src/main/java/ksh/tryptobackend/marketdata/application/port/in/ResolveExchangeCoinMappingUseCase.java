package ksh.tryptobackend.marketdata.application.port.in;

import java.util.Optional;

public interface ResolveExchangeCoinMappingUseCase {

    Optional<Long> resolve(String exchange, String symbol);
}
