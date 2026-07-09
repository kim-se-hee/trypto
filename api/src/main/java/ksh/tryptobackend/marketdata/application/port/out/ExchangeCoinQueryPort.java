package ksh.tryptobackend.marketdata.application.port.out;

import java.util.List;
import java.util.Optional;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import ksh.tryptobackend.marketdata.domain.model.ExchangeCoins;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinIdMap;

public interface ExchangeCoinQueryPort {

    Optional<ExchangeCoin> findById(Long exchangeCoinId);

    boolean existsByExchangeIdAndCoinId(Long exchangeId, Long coinId);

    ExchangeCoinIdMap findExchangeCoinIdMap(Long exchangeId, List<Long> coinIds);

    ExchangeCoins findByExchangeId(Long exchangeId);
}
