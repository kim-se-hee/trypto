package ksh.tryptobackend.marketdata.domain.vo;

public record MarketStatusNotification(
        Long exchangeId, Long exchangeCoinId, String symbol, String displayName, MarketStatus status) {}
