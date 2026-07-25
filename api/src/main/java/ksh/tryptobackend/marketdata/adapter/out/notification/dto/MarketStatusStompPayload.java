package ksh.tryptobackend.marketdata.adapter.out.notification.dto;

import ksh.tryptobackend.marketdata.domain.vo.MarketStatusNotification;

public record MarketStatusStompPayload(Long exchangeCoinId, String symbol, String displayName, String status) {

    public static MarketStatusStompPayload from(MarketStatusNotification notification) {
        return new MarketStatusStompPayload(
                notification.exchangeCoinId(),
                notification.symbol(),
                notification.displayName(),
                notification.status().name());
    }
}
