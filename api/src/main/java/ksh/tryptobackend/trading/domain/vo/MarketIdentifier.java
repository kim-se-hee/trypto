package ksh.tryptobackend.trading.domain.vo;

import java.util.Objects;

public record MarketIdentifier(String exchangeName, String marketSymbol) {

    public MarketIdentifier {
        Objects.requireNonNull(exchangeName);
        Objects.requireNonNull(marketSymbol);
    }

    public static MarketIdentifier of(
            String exchangeName, String coinSymbol, String baseCurrencySymbol) {
        return new MarketIdentifier(exchangeName, coinSymbol + "/" + baseCurrencySymbol);
    }
}
