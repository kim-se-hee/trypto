package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.domain.model.ExchangeCoinChain;

import java.util.List;
import java.util.Optional;

public interface ExchangeCoinChainQueryPort {

    Optional<ExchangeCoinChain> findByExchangeIdAndCoinIdAndChain(Long exchangeId, Long coinId, String chain);

    List<ExchangeCoinChain> findByExchangeIdAndCoinId(Long exchangeId, Long coinId);
}
