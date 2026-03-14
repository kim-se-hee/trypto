package ksh.tryptobackend.marketdata.application.port.in;

import java.util.Map;
import java.util.Set;

public interface FindExchangeNamesUseCase {

    Map<Long, String> findExchangeNames(Set<Long> exchangeIds);
}
