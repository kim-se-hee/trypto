package ksh.tryptobackend.marketdata.domain.model;

import ksh.tryptobackend.marketdata.domain.vo.MarketStatus;
import ksh.tryptobackend.marketdata.domain.vo.MarketStatusNotification;

public record ExchangeCoin(Long exchangeCoinId, Long exchangeId, Long coinId, String displayName, MarketStatus status) {

    public boolean isSuspended() {
        return status.isSuspended();
    }

    public boolean isTrading() {
        return status.isTrading();
    }

    public MarketStatusNotification toNotification(String symbol) {
        return new MarketStatusNotification(exchangeId, exchangeCoinId, symbol, displayName, status);
    }
}
