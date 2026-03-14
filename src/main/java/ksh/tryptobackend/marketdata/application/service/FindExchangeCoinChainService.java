package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindExchangeCoinChainUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeCoinChainResult;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinChainQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FindExchangeCoinChainService implements FindExchangeCoinChainUseCase {

    private final ExchangeCoinChainQueryPort exchangeCoinChainQueryPort;

    @Override
    public Optional<ExchangeCoinChainResult> findByExchangeIdAndCoinIdAndChain(
        Long exchangeId, Long coinId, String chain) {
        return exchangeCoinChainQueryPort.findByExchangeIdAndCoinIdAndChain(exchangeId, coinId, chain)
            .map(ecc -> new ExchangeCoinChainResult(ecc.isTagRequired()));
    }
}
