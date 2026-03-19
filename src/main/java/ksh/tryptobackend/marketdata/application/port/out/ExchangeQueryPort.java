package ksh.tryptobackend.marketdata.application.port.out;

import ksh.tryptobackend.marketdata.domain.model.Exchange;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSummary;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface ExchangeQueryPort {

    Optional<Exchange> findExchangeDetailById(Long exchangeId);

    Optional<ExchangeSummary> findExchangeSummaryById(Long exchangeId);

    List<Long> findAllExchangeIds();

    Map<Long, String> findNamesByIds(Set<Long> exchangeIds);

    boolean existsById(Long exchangeId);

    Optional<ExchangeSummary> findExchangeSummaryByName(String name);
}
