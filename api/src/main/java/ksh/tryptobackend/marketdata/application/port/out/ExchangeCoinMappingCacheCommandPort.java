package ksh.tryptobackend.marketdata.application.port.out;

import java.util.Map;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;

public interface ExchangeCoinMappingCacheCommandPort {

    void loadAll(Map<ExchangeSymbolKey, ExchangeCoinMapping> mappings);
}
