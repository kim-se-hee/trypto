package ksh.tryptobackend.marketdata.domain.model;

import java.time.Instant;
import java.util.regex.Pattern;
import ksh.tryptobackend.common.exception.CustomException;
import ksh.tryptobackend.common.exception.ErrorCode;

public record CandleFilter(
        String exchange, String coin, CandleInterval interval, int limit, Instant cursor) {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");
    private static final String MARKET_SYMBOL_SEPARATOR = "/";
    private static final int DEFAULT_LIMIT = 60;

    public static void validateIdentifiers(String exchange, String coin) {
        validateIdentifier(exchange, ErrorCode.INVALID_EXCHANGE_NAME);
        validateIdentifier(coin, ErrorCode.INVALID_COIN_SYMBOL);
    }

    public static CandleFilter of(
            String exchange,
            String coin,
            String interval,
            Integer limit,
            String cursor,
            String baseCurrencySymbol) {
        return new CandleFilter(
                exchange,
                coin + MARKET_SYMBOL_SEPARATOR + baseCurrencySymbol,
                CandleInterval.of(interval),
                limit != null ? limit : DEFAULT_LIMIT,
                parseCursor(cursor));
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
