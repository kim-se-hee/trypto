package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinIdMap;

import java.util.List;
import java.util.Optional;

public interface ExchangeCoinQueryPort {

    Optional<ExchangeCoinMapping> findById(Long exchangeCoinId);

    ExchangeCoinIdMap findExchangeCoinIdMap(Long exchangeId, List<Long> coinIds);
}
