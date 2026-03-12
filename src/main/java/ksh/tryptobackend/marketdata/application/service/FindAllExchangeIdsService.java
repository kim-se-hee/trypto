package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.marketdata.application.port.in.FindAllExchangeIdsUseCase;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FindAllExchangeIdsService implements FindAllExchangeIdsUseCase {

    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public List<Long> findAllExchangeIds() {
        return exchangeQueryPort.findAllExchangeIds();
    }
}
