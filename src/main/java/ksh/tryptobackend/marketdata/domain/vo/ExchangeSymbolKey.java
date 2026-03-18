package ksh.tryptobackend.marketdata.domain.vo;

import java.util.Objects;

public record ExchangeSymbolKey(
    String exchange,
    String symbol
) {

    public ExchangeSymbolKey {
        Objects.requireNonNull(exchange);
        Objects.requireNonNull(symbol);
    }

    public static ExchangeSymbolKey of(String exchange, String coinSymbol, String baseCurrencySymbol) {
        return new ExchangeSymbolKey(exchange, coinSymbol + "/" + baseCurrencySymbol);
    }
}
