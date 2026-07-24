package ksh.tryptobackend.marketdata.domain.vo;

import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import ksh.tryptobackend.marketdata.domain.model.MarketStatus;

public record MarketStatusNotification(
        Long exchangeId, Long exchangeCoinId, String symbol, String displayName, MarketStatus status) {

    public static MarketStatusNotification of(ExchangeCoin exchangeCoin, String symbol) {
        return new MarketStatusNotification(
                exchangeCoin.exchangeId(),
                exchangeCoin.exchangeCoinId(),
                symbol,
                exchangeCoin.displayName(),
                exchangeCoin.status());
    }
}
