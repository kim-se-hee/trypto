package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;

import java.util.Map;

public interface ExchangeCoinMappingCacheCommandPort {

    void loadAll(Map<ExchangeSymbolKey, ExchangeCoinMapping> mappings);
}
