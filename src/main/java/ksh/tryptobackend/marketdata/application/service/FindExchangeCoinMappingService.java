package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FindExchangeCoinMappingService implements FindExchangeCoinMappingUseCase {

    private final ExchangeCoinQueryPort exchangeCoinQueryPort;

    @Override
    public Optional<ExchangeCoinMappingResult> findById(Long exchangeCoinId) {
        return exchangeCoinQueryPort.findById(exchangeCoinId)
            .map(ec -> new ExchangeCoinMappingResult(ec.exchangeCoinId(), ec.exchangeId(), ec.coinId()));
    }

    @Override
    public Map<Long, Long> findExchangeCoinIdMap(Long exchangeId, List<Long> coinIds) {
        return exchangeCoinQueryPort.findExchangeCoinIdMap(exchangeId, coinIds).toMap();
    }
}
