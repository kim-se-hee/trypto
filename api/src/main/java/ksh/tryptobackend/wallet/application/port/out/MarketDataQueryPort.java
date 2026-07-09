package ksh.tryptobackend.wallet.application.port.out;

import java.util.Map;
import java.util.Set;
import ksh.tryptobackend.wallet.domain.vo.BaseCurrency;

public interface MarketDataQueryPort {

    BaseCurrency getBaseCurrency(Long exchangeId);

    Map<Long, String> findCoinSymbols(Set<Long> coinIds);
}
