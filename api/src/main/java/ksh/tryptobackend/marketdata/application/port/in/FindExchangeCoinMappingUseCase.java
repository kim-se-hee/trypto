package ksh.tryptobackend.marketdata.application.port.in;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;

public interface FindExchangeCoinMappingUseCase {

    Optional<ExchangeCoinMappingResult> findById(Long exchangeCoinId);

    Map<Long, Long> findExchangeCoinIdMap(Long exchangeId, List<Long> coinIds);
}
