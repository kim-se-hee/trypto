package ksh.tryptobackend.marketdata.application.port.in;

import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface FindExchangeCoinMappingUseCase {

    Optional<ExchangeCoinMappingResult> findById(Long exchangeCoinId);

    Map<Long, Long> findExchangeCoinIdMap(Long exchangeId, List<Long> coinIds);
}
