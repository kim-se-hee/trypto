package ksh.tryptobackend.marketdata.application.service;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinMappingUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinMappingResult;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinQueryPort;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindExchangeCoinMappingService implements FindExchangeCoinMappingUseCase {

    private final ExchangeCoinQueryPort exchangeCoinQueryPort;

    @Override
    public Optional<ExchangeCoinMappingResult> findById(Long exchangeCoinId) {
        return exchangeCoinQueryPort.findById(exchangeCoinId).map(this::toResult);
    }

    @Override
    public List<ExchangeCoinMappingResult> findExchangeCoinMappings(Long exchangeId, List<Long> coinIds) {
        return exchangeCoinQueryPort.findByExchangeIdAndCoinIds(exchangeId, coinIds).stream()
                .map(this::toResult)
                .toList();
    }

    private ExchangeCoinMappingResult toResult(ExchangeCoin exchangeCoin) {
        return new ExchangeCoinMappingResult(
                exchangeCoin.exchangeCoinId(),
                exchangeCoin.exchangeId(),
                exchangeCoin.coinId(),
                exchangeCoin.isSuspended());
    }
}
