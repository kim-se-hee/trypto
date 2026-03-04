package ksh.tryptobackend.ranking.application.port.out;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ExchangeCoinQueryPort {

    Optional<Long> findExchangeCoinId(Long exchangeId, Long coinId);

    Map<Long, Long> findExchangeCoinIdsByExchangeIdAndCoinIds(Long exchangeId, List<Long> coinIds);
}
