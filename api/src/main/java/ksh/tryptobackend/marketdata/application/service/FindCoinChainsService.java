package ksh.tryptobackend.marketdata.application.service;

import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindCoinChainsUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.CoinChainResult;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinChainQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeCoinQueryPort;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoinChain;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindCoinChainsService implements FindCoinChainsUseCase {

    private final ExchangeCoinChainQueryPort exchangeCoinChainQueryPort;
    private final ExchangeCoinQueryPort exchangeCoinQueryPort;

    @Override
    public List<CoinChainResult> findCoinChains(Long exchangeId, Long coinId) {
        List<ExchangeCoinChain> chains =
                exchangeCoinChainQueryPort.findByExchangeIdAndCoinId(exchangeId, coinId);
        if (chains.isEmpty()) {
            validateExchangeCoinExists(exchangeId, coinId);
        }
        return toResults(chains);
    }

    private void validateExchangeCoinExists(Long exchangeId, Long coinId) {
        if (!exchangeCoinQueryPort.existsByExchangeIdAndCoinId(exchangeId, coinId)) {
            throw new CustomException(ErrorCode.EXCHANGE_COIN_NOT_FOUND);
        }
    }

    private List<CoinChainResult> toResults(List<ExchangeCoinChain> chains) {
        return chains.stream()
                .map(
                        chain ->
                                new CoinChainResult(
                                        chain.getExchangeCoinChainId(),
                                        chain.getChain(),
                                        chain.isTagRequired()))
                .toList();
    }
}
