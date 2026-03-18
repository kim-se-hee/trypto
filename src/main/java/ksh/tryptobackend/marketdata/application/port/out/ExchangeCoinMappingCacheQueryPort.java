package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;

import java.util.Optional;

public interface ExchangeCoinMappingCacheQueryPort {

    Optional<ExchangeCoinMapping> resolve(String exchange, String symbol);
}
