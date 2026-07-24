package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.GetMarketStatusUseCase;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinQueryPort;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetMarketStatusService implements GetMarketStatusUseCase {

    private final ExchangeCoinQueryPort exchangeCoinQueryPort;

    @Override
    @Transactional(readOnly = true)
    public boolean isSuspended(Long exchangeCoinId) {
        return exchangeCoinQueryPort
                .findById(exchangeCoinId)
                .map(ExchangeCoin::isSuspended)
                .orElse(true);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSuspended(Long exchangeId, Long coinId) {
        return exchangeCoinQueryPort
                .findByExchangeIdAndCoinId(exchangeId, coinId)
                .map(ExchangeCoin::isSuspended)
                .orElse(false);
    }
}
