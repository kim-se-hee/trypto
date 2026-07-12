package ksh.tryptobackend.marketdata.application.service;

import java.util.Optional;
import ksh.tryptobackend.marketdata.application.port.in.FindExchangeDetailUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.result.ExchangeDetailResult;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindExchangeDetailService implements FindExchangeDetailUseCase {

    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public Optional<ExchangeDetailResult> findExchangeDetail(Long exchangeId) {
        return exchangeQueryPort
                .findExchangeDetailById(exchangeId)
                .map(exchange -> new ExchangeDetailResult(
                        exchange.getName(),
                        exchange.getBaseCurrencyCoinId(),
                        exchange.isDomestic(),
                        exchange.getFeeRate()));
    }
}
