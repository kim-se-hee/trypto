package ksh.tryptobackend.marketdata.domain.event;

import ksh.tryptobackend.marketdata.domain.model.ExchangeCoin;
import ksh.tryptobackend.marketdata.domain.vo.MarketStatusNotification;

public final class MarketStatusChangedEvent {

    private final ExchangeCoin exchangeCoin;
    private final String symbol;

    private MarketStatusChangedEvent(ExchangeCoin exchangeCoin, String symbol) {
        this.exchangeCoin = exchangeCoin;
        this.symbol = symbol;
    }

    public static MarketStatusChangedEvent of(ExchangeCoin exchangeCoin, String symbol) {
        return new MarketStatusChangedEvent(exchangeCoin, symbol);
    }

    public MarketStatusNotification toNotification() {
        return exchangeCoin.toNotification(symbol);
    }
}
