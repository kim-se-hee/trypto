package ksh.tryptobackend.marketdata.domain.model;

import ksh.tryptobackend.marketdata.domain.vo.MarketStatus;

public record ExchangeCoin(Long exchangeCoinId, Long exchangeId, Long coinId, String displayName, MarketStatus status) {

    public boolean isSuspended() {
        return status.isSuspended();
    }

    public boolean isTrading() {
        return status.isTrading();
    }
}
