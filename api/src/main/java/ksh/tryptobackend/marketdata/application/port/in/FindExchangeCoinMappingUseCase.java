package ksh.tryptobackend.marketdata.application.port.in;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;

public interface FindExchangeCoinMappingUseCase {

    Optional<ExchangeCoinMappingResult> findById(Long exchangeCoinId);

    List<ExchangeCoinMappingResult> findExchangeCoinMappings(Long exchangeId, List<Long> coinIds);
}
