package ksh.tryptobackend.regretanalysis.application.port.out;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import ksh.tryptobackend.regretanalysis.domain.vo.AnalysisExchange;
import ksh.tryptobackend.regretanalysis.domain.vo.BtcDailyPrices;

public interface MarketDataQueryPort {

    AnalysisExchange getExchange(Long exchangeId);

    Map<Long, String> findCoinSymbols(Set<Long> coinIds);

    BtcDailyPrices findBtcDailyPrices(LocalDate startDate, LocalDate endDate, String currency);
}
