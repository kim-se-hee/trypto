package ksh.tryptobackend.marketdata.application.service;

import java.util.List;
import ksh.tryptobackend.marketdata.application.port.in.FindAllExchangeIdsUseCase;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindAllExchangeIdsService implements FindAllExchangeIdsUseCase {

    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public List<Long> findAllExchangeIds() {
        return exchangeQueryPort.findAllExchangeIds();
    }
}
