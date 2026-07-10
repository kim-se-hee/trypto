package ksh.tryptobackend.regretanalysis.application.port.out;

import java.util.Map;
import java.util.Set;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;

public interface MarketDataQueryPort {

    AnalysisExchange getExchange(Long exchangeId);

    Map<Long, String> findCoinSymbols(Set<Long> coinIds);
}
