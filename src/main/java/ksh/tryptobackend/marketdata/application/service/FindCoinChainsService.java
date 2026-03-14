package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinChainsUseCase;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinChainQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinQueryPort;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoinChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindCoinChainsService implements FindCoinChainsUseCase {

    private final ExchangeCoinChainQueryPort exchangeCoinChainQueryPort;
    private final ExchangeCoinQueryPort exchangeCoinQueryPort;

    @Override
    public List<ExchangeCoinChain> findByExchangeIdAndCoinId(Long exchangeId, Long coinId) {
        validateExchangeCoinExists(exchangeId, coinId);
        return exchangeCoinChainQueryPort.findByExchangeIdAndCoinId(exchangeId, coinId);
    }

    private void validateExchangeCoinExists(Long exchangeId, Long coinId) {
        if (!exchangeCoinQueryPort.existsByExchangeIdAndCoinId(exchangeId, coinId)) {
            throw new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND);
        }
    }
}
