package ksh.tryptobackend.marketdata.application.port.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoinChain;

public interface ExchangeCoinChainQueryPort {

    Optional<ExchangeCoinChain> findByExchangeIdAndCoinIdAndChain(
            Long exchangeId, Long coinId, String chain);

    List<ExchangeCoinChain> findByExchangeIdAndCoinId(Long exchangeId, Long coinId);
}
