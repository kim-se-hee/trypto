package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeCoinMapping;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ExchangeCoinQueryPort {

    Optional<ExchangeCoinMapping> findById(Long exchangeCoinId);

    Map<Long, Long> findExchangeCoinIdMap(Long exchangeId, List<Long> coinIds);
}
