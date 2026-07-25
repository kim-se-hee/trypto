package ksh.tryptocollector.model;

public record MarketStatusChanged(Exchange exchange, String symbol, String displayName, MarketStatus status) {

    public static MarketStatusChanged trading(Exchange exchange, MarketInfo info) {
        return new MarketStatusChanged(exchange, info.pair(), info.displayName(), MarketStatus.TRADING);
    }

    public static MarketStatusChanged suspended(Exchange exchange, MarketInfo info) {
        return new MarketStatusChanged(exchange, info.pair(), info.displayName(), MarketStatus.SUSPENDED);
    }
}
