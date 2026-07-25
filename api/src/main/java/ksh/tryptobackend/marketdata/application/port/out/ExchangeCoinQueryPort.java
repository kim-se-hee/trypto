package ksh.tryptobackend.marketdata.application.port.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoins;

public interface ExchangeCoinQueryPort {

    Optional<ExchangeCoin> findById(Long exchangeCoinId);

    Optional<ExchangeCoin> findByExchangeIdAndCoinId(Long exchangeId, Long coinId);

    boolean existsByExchangeIdAndCoinId(Long exchangeId, Long coinId);

    List<ExchangeCoin> findByExchangeIdAndCoinIds(Long exchangeId, List<Long> coinIds);

    ExchangeCoins findByExchangeId(Long exchangeId);
}
