package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindExchangeNamesUseCase;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.dto.ExchangeSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FindExchangeNamesService implements FindExchangeNamesUseCase {

    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public Map<Long, String> findExchangeNames(Set<Long> exchangeIds) {
        return exchangeIds.stream()
            .collect(Collectors.toMap(
                id -> id,
                id -> exchangeQueryPort.findExchangeSummaryById(id)
                    .map(ExchangeSummary::name)
                    .orElse("")
            ));
    }
}
