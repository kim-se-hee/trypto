package ksh.tryptobackend.marketdata.application.service;

import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.FindCandlesUseCase;
import ksh.tryptobackend.marketdata.application.port.in.dto.query.FindCandlesQuery;
import ksh.tryptobackend.marketdata.application.port.out.CandleQueryPort;
import ksh.tryptobackend.marketdata.application.port.out.ExchangeQueryPort;
import ksh.tryptobackend.marketdata.domain.model.Candle;
import ksh.tryptobackend.marketdata.domain.model.CandleFilter;
import ksh.tryptobackend.marketdata.domain.model.CandleInterval;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class FindCandlesService implements FindCandlesUseCase {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final int DEFAULT_LIMIT = 60;

    private final CandleQueryPort candleQueryPort;
    private final ExchangeQueryPort exchangeQueryPort;

    @Override
    public List<Candle> findCandles(FindCandlesQuery query) {
        validateIdentifier(query.exchange(), ErrorCode.INVALID_EXCHANGE_NAME);
        validateIdentifier(query.coin(), ErrorCode.INVALID_COIN_SYMBOL);
        CandleFilter filter = toCandleFilter(query);
        return candleQueryPort.findByFilter(filter);
    }

    private void validateIdentifier(String value, ErrorCode errorCode) {
        if (value == null || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new CustomException(errorCode);
        }
    }

    private CandleFilter toCandleFilter(FindCandlesQuery query) {
        String influxCoin = resolveInfluxCoin(query);
        CandleInterval interval = CandleInterval.of(query.interval());
        int limit = query.limit() != null ? query.limit() : DEFAULT_LIMIT;
        Instant cursor = parseCursor(query.cursor());
        return new CandleFilter(query.exchange(), influxCoin, interval, limit, cursor);
    }

    private String resolveInfluxCoin(FindCandlesQuery query) {
        ExchangeSummary exchange = exchangeQueryPort.findExchangeSummaryByName(query.exchange())
            .orElseThrow(() -> new CustomException(ErrorCode.EXCHANGE_NOT_FOUND));
        return query.coin() + "/" + exchange.baseCurrencySymbol();
    }

    private Instant parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        return Instant.parse(cursor);
    }
}
