package ksh.tryptobackend.marketdata.application.service;

import java.util.List;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindCandlesUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.query.FindCandlesQuery;
import ksh.tryptobackend.marketdata.application.port.out.CandleQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import ksh.tryptobackend.marketdata.domain.model.CandleFilter;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FindCandlesService implements FindCandlesUseCase {

    private final CandleQueryPort candleQueryPort;
    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public List<Candle> findCandles(FindCandlesQuery query) {
        CandleFilter.validateIdentifiers(query);
        ExchangeSummary exchange =
                exchangeQueryPort
                        .findExchangeSummaryByName(query.exchange())
                        .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        CandleFilter filter = CandleFilter.of(query, exchange.baseCurrencySymbol());
        return candleQueryPort.findByFilter(filter);
    }
}
