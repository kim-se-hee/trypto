package ksh.tryptobackend.marketdata.application.port.out;

import java.util.Optional;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;

public interface ExchangeCoinMappingCacheQueryPort {

    Optional<ExchangeCoinMapping> resolve(String exchange, String symbol);
}
