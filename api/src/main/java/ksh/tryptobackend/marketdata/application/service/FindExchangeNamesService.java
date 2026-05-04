package ksh.tryptobackend.marketdata.application.service;

import java.util.Map;
import java.util.Set;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeNamesUseCase;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindExchangeNamesService implements FindExchangeNamesUseCase {

    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public Map<Long, String> findExchangeNames(Set<Long> exchangeIds) {
        return exchangeQueryPort.findNamesByIds(exchangeIds);
    }
}
