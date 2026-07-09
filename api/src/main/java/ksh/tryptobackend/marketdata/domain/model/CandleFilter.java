package ksh.tryptobackend.marketdata.domain.model;

import java.time.Instant;
import java.util.regex.Pattern;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;
import ksh.tryptobackend.marketdata.application.port.in.dto.query.FindCandlesQuery;

public record CandleFilter(
        String exchange, String coin, CandleInterval interval, int limit, Instant cursor) {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final String MARKET_SYMBOL_SEPARATOR = "/";
    private static final int DEFAULT_LIMIT = 60;

    public static void validateIdentifiers(FindCandlesQuery query) {
        validateIdentifier(query.exchange(), ErrorCode.INVALID_EXCHANGE_NAME);
        validateIdentifier(query.coin(), ErrorCode.INVALID_COIN_SYMBOL);
    }

    public static CandleFilter of(FindCandlesQuery query, String baseCurrencySymbol) {
        return new CandleFilter(
                query.exchange(),
                query.coin() + MARKET_SYMBOL_SEPARATOR + baseCurrencySymbol,
                CandleInterval.of(query.interval()),
                query.limit() != null ? query.limit() : DEFAULT_LIMIT,
                parseCursor(query.cursor()));
    }

    private static void validateIdentifier(String value, ErrorCode errorCode) {
        if (value == null || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new CustomException(errorCode);
        }
    }

    private static Instant parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        return Instant.parse(cursor);
    }
}
