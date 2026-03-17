package ksh.tryptobackend.trading.application.port.out;

import ksh.tryptobackend.trading.domain.vo.ExchangeSymbolKey;

import java.util.Map;

public interface ExchangeCoinMappingCacheCommandPort {

    void loadAll(Map<ExchangeSymbolKey, Long> mappings);
}
