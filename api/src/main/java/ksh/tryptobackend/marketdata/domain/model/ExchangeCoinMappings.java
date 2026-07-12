package ksh.tryptobackend.marketdata.domain.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import ksh.tryptobackend.marketdata.domain.vo.CoinSymbols;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeCoinMapping;
import ksh.tryptobackend.marketdata.domain.vo.ExchangeSymbolKey;

public class ExchangeCoinMappings {

    private final Map<ExchangeSymbolKey, ExchangeCoinMapping> values;

    private ExchangeCoinMappings(Map<ExchangeSymbolKey, ExchangeCoinMapping> values) {
        this.values = values;
    }

    public static ExchangeCoinMappings empty() {
        return new ExchangeCoinMappings(Map.of());
    }

    public ExchangeCoinMappings add(
            Exchange exchange, String baseCurrencySymbol, ExchangeCoins coins, CoinSymbols coinSymbols) {
        if (baseCurrencySymbol == null) {
            return this;
        }
        Map<ExchangeSymbolKey, ExchangeCoinMapping> merged = new HashMap<>(values);
        for (ExchangeCoin coin : coins.values()) {
            String coinSymbol = coinSymbols.getSymbol(coin.coinId());
            merged.put(
                    ExchangeSymbolKey.of(exchange.getName(), coinSymbol, baseCurrencySymbol),
                    new ExchangeCoinMapping(
                            coin.exchangeCoinId(), exchange.getExchangeId(), coin.coinId(), coinSymbol));
        }
        return new ExchangeCoinMappings(merged);
    }

    public Map<ExchangeSymbolKey, ExchangeCoinMapping> toMap() {
        return Map.copyOf(values);
    }

    public int size() {
        return values.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExchangeCoinMappings that = (ExchangeCoinMappings) o;
        return Objects.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(values);
    }
}
