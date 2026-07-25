package ksh.tryptobackend.marketdata.domain.model;

import ksh.tryptobackend.common.domain.model.AggregateRoot;
import ksh.tryptobackend.common.event.MarketSuspendedEvent;
import ksh.tryptobackend.marketdata.domain.event.MarketStatusChangedEvent;
import ksh.tryptobackend.marketdata.domain.vo.MarketStatus;
import ksh.tryptobackend.marketdata.domain.vo.MarketStatusNotification;

public class ExchangeCoin extends AggregateRoot {

    private Long exchangeCoinId;
    private final Long exchangeId;
    private final Long coinId;
    private String displayName;
    private MarketStatus status;

    public ExchangeCoin(Long exchangeCoinId, Long exchangeId, Long coinId, String displayName, MarketStatus status) {
        this.exchangeCoinId = exchangeCoinId;
        this.exchangeId = exchangeId;
        this.coinId = coinId;
        this.displayName = displayName;
        this.status = status;
    }

    public static ExchangeCoin newListing(Long exchangeId, Long coinId, String displayName, String symbol) {
        ExchangeCoin exchangeCoin = new ExchangeCoin(null, exchangeId, coinId, displayName, MarketStatus.TRADING);
        exchangeCoin.registerEvent(MarketStatusChangedEvent.of(exchangeCoin, symbol));
        return exchangeCoin;
    }

    public void startTrading(String displayName, String symbol) {
        this.displayName = displayName;
        if (status.isSuspended()) {
            this.status = MarketStatus.TRADING;
            registerEvent(MarketStatusChangedEvent.of(this, symbol));
        }
    }

    public void suspend(String symbol) {
        if (status.isTrading()) {
            this.status = MarketStatus.SUSPENDED;
            registerEvent(new MarketSuspendedEvent(exchangeCoinId));
            registerEvent(MarketStatusChangedEvent.of(this, symbol));
        }
    }

    public void assignId(Long exchangeCoinId) {
        if (this.exchangeCoinId != null) {
            throw new IllegalStateException("이미 식별자가 부여된 상장코인입니다 id=" + this.exchangeCoinId);
        }
        this.exchangeCoinId = exchangeCoinId;
    }

    public boolean isSuspended() {
        return status.isSuspended();
    }

    public boolean isTrading() {
        return status.isTrading();
    }

    public MarketStatusNotification toNotification(String symbol) {
        return new MarketStatusNotification(exchangeId, exchangeCoinId, symbol, displayName, status);
    }

    public Long exchangeCoinId() {
        return exchangeCoinId;
    }

    public Long exchangeId() {
        return exchangeId;
    }

    public Long coinId() {
        return coinId;
    }

    public String displayName() {
        return displayName;
    }

    public MarketStatus status() {
        return status;
    }
}
